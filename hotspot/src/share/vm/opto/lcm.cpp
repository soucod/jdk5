#ifdef USE_PRAGMA_IDENT_SRC
#pragma ident "@(#)lcm.cpp	1.85 04/04/30 16:41:11 JVM"
#endif
/*
 * Copyright 2004 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

// Optimization - Graph Style

#include "incls/_precompiled.incl"
#include "incls/_lcm.cpp.incl"

//------------------------------remove_dead------------------------------------
// Recursively kill a dead Node.
void Block::remove_dead( Node *dead ) {
  // Smash all inputs to 'dead', isolating him completely
  for( uint i = 0; i < dead->req(); i++ ) {
    Node *in = dead->in(i);
    if( in ) {                 // Points to something?
      dead->set_req(i,NULL);   // Kill the edge
      if( in->outcnt() == 0 ) {// Made input go dead?
        remove_dead(in);       // Recursively remove
      }
    }
  }
}

//------------------------------implicit_null_check----------------------------
// Detect implicit-null-check opportunities.  Basically, find NULL checks 
// with suitable memory ops nearby.  Use the memory op to do the NULL check.
// I can generate a memory op if there is not one nearby.
void Block::implicit_null_check(Block_Array &bbs, GrowableArray<uint> &latency, Node *proj, Node *val) {
  // Assume if null check need for 0 offset then always needed
  // Intel solaris doesn't support any null checks yet and no
  // mechanism exists (yet) to set the switches at an os_cpu level
  if( !ImplicitNullChecks || MacroAssembler::needs_explicit_null_check(0)) return;

  // Make sure the ptr-is-null path appears to be uncommon!
  float f = end()->is_Mach()->is_MachIf()->_prob;
  if( proj->Opcode() == Op_IfTrue ) f = 1.0f - f;
  if( f > PROB_UNLIKELY_MAG(4) ) return;

  uint bidx = 0;                // Capture index of value into memop
  bool was_store;               // Memory op is a store op

  // Search the successor block for a load or store who's base value is also
  // the tested value.  There may be several.
  Node_List *out = new Node_List(Thread::current()->resource_area());
  MachNode *best = NULL;        // Best found so far
  for (DUIterator i = val->outs(); val->has_out(i); i++) {
    MachNode *mach = val->out(i)->is_Mach();
    if( !mach ) continue;
    was_store = false;
    switch( mach->ideal_Opcode() ) {
    case Op_LoadB:
    case Op_LoadC:
    case Op_LoadD:
    case Op_LoadF:
    case Op_LoadI:
    case Op_LoadL:
    case Op_LoadP:
    case Op_LoadS:
    case Op_LoadKlass:
    case Op_LoadRange:
    case Op_LoadD_unaligned:
    case Op_LoadL_unaligned:
      break;
    case Op_StoreB:
    case Op_StoreC:
    case Op_StoreCM:
    case Op_StoreD:
    case Op_StoreF:
    case Op_StoreI:
    case Op_StoreL:
    case Op_StoreP:
      was_store = true;         // Memory op is a store op
      // Stores will have their address in slot 2 (memory in slot 1).
      // If the value being nul-checked is in another slot, it means we
      // are storing the checked value, which does NOT check the value!
      if( mach->in(2) != val ) continue;
      break;                    // Found a memory op?
    case Op_StrComp:            
      // Not a legit memory op for implicit null check regardless of 
      // embedded loads
      continue;
    default:                    // Also check for embedded loads
      if( !mach->check_for_anti_dependence() )
        continue;               // Not an memory op; skip it
      break;
    }
    // check if the offset is not too high for implicit exception
    {
      intptr_t offset = 0;
      const TypePtr *adr_type = NULL;  // Do not need this return value here
      const Node* base = mach->get_base_and_disp(offset, adr_type);
      if (base == NULL || base == NodeSentinel) {
        // cannot reason about it; is probably not implicit null exception
      } else {
        const TypePtr* tptr = base->bottom_type()->is_ptr();
        // Give up if offset is not a compile-time constant
        if( offset == Type::OffsetBot || tptr->_offset == Type::OffsetBot )
          continue;
        offset += tptr->_offset; // correct if base is offseted
        if( MacroAssembler::needs_explicit_null_check(offset) ) 
          continue;             // Give up is reference is beyond 4K page size
      }
    }

    // Check ctrl input to see if the null-check dominates the memory op
    Block *cb = bbs[mach->_idx];
    cb = cb->_idom;             // Always hoist at least 1 block
    if( !was_store ) {          // Stores can be hoisted only one block
      while( cb->_dom_depth > _dom_depth )
        cb = cb->_idom;         // Hoist loads as far as we want
    }
    if( cb != this ) continue;

    // Found a memory user; see if it can be hoisted to check-block
    uint vidx = 0;              // Capture index of value into memop
    uint j;
    for( j = mach->req()-1; j > 0; j-- ) {
      if( mach->in(j) == val ) vidx = j;
      // Block of memory-op input
      Block *inb = bbs[mach->in(j)->_idx];
      Block *b = this;          // Start from nul check
      while( b != inb && b->_dom_depth > inb->_dom_depth )
        b = b->_idom;           // search upwards for input
      // See if input dominates null check
      if( b != inb )
        break;
    }
    if( j > 0 ) 
      continue;
    Block *mb = bbs[mach->_idx]; 
    // Hoisting stores requires more checks for the anti-dependence case.
    // Give up hoisting if we have to move the store past any load.
    if( was_store ) {
      Block *b = mb;            // Start searching here for a local load
      // mach use (faulting) trying to hoist
      // n might be blocker to hoisting
      while( b != this ) {
        uint k;
        for( k = 1; k < b->_nodes.size(); k++ ) {
          Node *n = b->_nodes[k];
          if( n->check_for_anti_dependence() && 
              n->in(LoadNode::Memory) == mach->in(StoreNode::Memory) )
            break;              // Found anti-dependent load
        }
        if( k < b->_nodes.size() )
          break;                // Found anti-dependent load
        // Make sure control does not do a merge (would have to check allpaths)
        if( b->num_preds() != 2 ) break;
        b = bbs[b->pred(1)->_idx]; // Move up to predecessor block
      }
      if( b != this ) continue;
    }

    // Make sure this memory op is not already being used for a NullCheck
    MachNode *e = mb->end()->is_Mach();
    if( e && e->is_MachNullCheck() && e->in(1) == mach )
      continue;                 // Already being used as a NULL check

    // Found a candidate!  Pick one with least dom depth - the highest 
    // in the dom tree should be closest to the null check.
    if( !best || 
        bbs[mach->_idx]->_dom_depth < bbs[best->_idx]->_dom_depth ) {
      best = mach;
      bidx = vidx;

    }
  }
  // No candidate!
  if( !best ) return;

  // ---- Found an implicit null check
  extern int implicit_null_checks;
  implicit_null_checks++;

  // Hoist the memory candidate up to the end of the test block.
  Block *old_block = bbs[best->_idx];
  old_block->find_remove(best);
  add_inst(best);
  bbs.map(best->_idx,this);

  // Move the control dependence
  if (best->in(0) && best->in(0) == old_block->_nodes[0])
    best->set_req(0, _nodes[0]);

  // Check for flag-killing projections that also need to be hoisted
  // Should be DU safe because no edge updates.
  for (DUIterator_Fast jmax, j = best->fast_outs(jmax); j < jmax; j++) {
    Node* n = best->fast_out(j);
    if( n->Opcode() == Op_MachProj ) {
      bbs[n->_idx]->find_remove(n);
      add_inst(n);
      bbs.map(n->_idx,this);
    }
  }

  // proj==Op_True --> ne test; proj==Op_False --> eq test.
  // One of two graph shapes got matched:
  //   (IfTrue  (If (Bool NE (CmpP ptr NULL))))
  //   (IfFalse (If (Bool EQ (CmpP ptr NULL))))
  // NULL checks are always branch-if-eq.  If we see a IfTrue projection
  // then we are replacing a 'ne' test with a 'eq' NULL check test.
  // We need to flip the projections to keep the same semantics.
  if( proj->Opcode() == Op_IfTrue ) {
    // Swap order of projections in basic block to swap branch targets
    Node *tmp1 = _nodes[end_idx()+1];
    Node *tmp2 = _nodes[end_idx()+2];
    _nodes.map(end_idx()+1, tmp2);
    _nodes.map(end_idx()+2, tmp1);    
    Node *tmp = new (1) Node(1);
    tmp1->replace_by(tmp);
    tmp2->replace_by(tmp1);
    tmp->replace_by(tmp2);
  }

  // Remove the existing null check; use a new implicit null check instead.
  // Since schedule-local needs precise def-use info, we need to correct
  // it as well.
  Node *old_tst = proj->in(0);
  MachNode *nul_chk = new MachNullCheckNode(old_tst->in(0),best,bidx);
  _nodes.map(end_idx(),nul_chk);
  bbs.map(nul_chk->_idx,this);
  // Redirect users of old_test to nul_chk
  for (DUIterator_Last i2min, i2 = old_tst->last_outs(i2min); i2 >= i2min; --i2)
    old_tst->last_out(i2)->set_req(0, nul_chk);
  // Clean-up any dead code
  for (uint i3 = 0; i3 < old_tst->req(); i3++)
    old_tst->set_req(i3, NULL);
  latency.at_put_grow(nul_chk->_idx, nul_chk->latency_from_uses(bbs, latency));
  latency.at_put_grow(best   ->_idx, best   ->latency_from_uses(bbs, latency));

#ifndef PRODUCT
  if (TraceOptoPipelining) {
    tty->print("# implicit_null_check: latency %4d for ", latency.at_grow(best->_idx));
    best->fast_dump();
    tty->print("# implicit_null_check: latency %4d for ", latency.at_grow(nul_chk->_idx));
    nul_chk->fast_dump();
  }
#endif
}


//------------------------------select-----------------------------------------
// Select a nice fellow from the worklist to schedule next. If there is only
// one choice, then use it. Projections take top priority for correctness
// reasons - if I see a projection, then it is next.  There are a number of
// other special cases, for instructions that consume condition codes, et al.
// These are chosen immediately. Some instructions are required to immediately
// precede the last instruction in the block, and these are taken last. Of the
// remaining cases (most), choose the instruction with the greatest latency
// (that is, the most number of pseudo-cycles required to the end of the
// routine). If there is a tie, choose the instruction with the most inputs.
Node *Block::select(Node_List &worklist, Block_Array &bbs, int *ready_cnt, VectorSet &next_call, uint sched_slot, GrowableArray<uint> &node_latency) {

  // If only a single entry on the stack, use it
  uint cnt = worklist.size();
  if (cnt == 1) {
    Node *n = worklist[0];
    worklist.map(0,worklist.pop());
    return n;
  }

  uint choice  = 0; // Bigger is most important
  uint latency = 0; // Bigger is scheduled first
  uint score   = 0; // Bigger is better
  uint idx;         // Index in worklist

  for( uint i=0; i<cnt; i++ ) { // Inspect entire worklist
    Node *n = worklist[i];      // Get Node on worklist

    const MachNode *mach = n->is_Mach();
    int iop = mach ? mach->ideal_Opcode() : 0;
    if( n->is_Proj() ||         // Projections always win
        n->Opcode()== Op_Con || // So does constant 'Top'
        iop == Op_CreateEx ||   // Create-exception must start block
        iop == Op_CheckCastPP
        ) {  
      worklist.map(i,worklist.pop());
      return n;
    }

    // Final call in a block must be adjacent to 'catch'
    Node *e = end();
    if( e->is_Catch() && e->in(0)->in(0) == n )
      continue;

    // Memory op for an implicit null check has to be at the end of the block
    MachNode *emach = e->is_Mach();
    if( emach && emach->is_MachNullCheck() && emach->in(1) == n )
      continue;

    uint n_choice  = 2;

    // See if this instruction is consumed by a branch. If so, then (as the
    // branch is the last instruction in the basic block) force it to the
    // end of the basic block
    if ( must_clone[iop] ) {
      // See if any use is a branch
      bool found_machif = false;

      for (DUIterator_Fast jmax, j = n->fast_outs(jmax); j < jmax; j++) {
        Node* use = n->fast_out(j);

        // The use is a conditional branch, make them adjacent
        MachNode *muse = use->is_Mach();
        if (muse && muse->is_MachIf() && bbs[use->_idx]==this ) {
          found_machif = true;
          break;
        }

        // More than this instruction pending for successor to be ready,
        // don't choose this if other opportunities are ready
        if (ready_cnt[use->_idx] > 1)
          n_choice = 1;
      }
  
      // loop terminated, prefer not to use this instruction
      if (found_machif)
        continue;
    }

    // See if this has a predecessor that is "must_clone", i.e. sets the
    // condition code. If so, choose this first
    for (uint j = 0; j < n->req() ; j++) {
      Node *inn = n->in(j);
      if (inn) {
        MachNode *inm = inn->is_Mach(); 
        if (inm && must_clone[inm->ideal_Opcode()] ) {
          n_choice = 3;
          break;
        }
      }
    }

    uint n_latency = node_latency.at_grow(n->_idx);
    uint n_score   = n->req();   // Many inputs get high score to break ties

    // Keep best latency found
    if( choice < n_choice ||
        ( choice == n_choice &&
          ( latency < n_latency ||
            ( latency == n_latency &&
              ( score < n_score ))))) {
      choice  = n_choice;
      latency = n_latency;
      score   = n_score;
      idx     = i;               // Also keep index in worklist
    }
  } // End of for all ready nodes in worklist

  Node *n = worklist[idx];      // Get the winner

  worklist.map(idx,worklist.pop());     // Compress worklist
  return n;
}


//------------------------------set_next_call----------------------------------
void Block::set_next_call( Node *n, VectorSet &next_call, Block_Array &bbs ) {
  if( next_call.test_set(n->_idx) ) return;
  for( uint i=0; i<n->len(); i++ ) {
    Node *m = n->in(i);
    if( !m ) continue;  // must see all nodes in block that precede call
    if( bbs[m->_idx] == this ) 
      set_next_call( m, next_call, bbs );
  }
}

//------------------------------needed_for_next_call---------------------------
// Set the flag 'next_call' for each Node that is needed for the next call to
// be scheduled.  This flag lets me bias scheduling so Nodes needed for the 
// next subroutine call get priority - basically it moves things NOT needed
// for the next call till after the call.  This prevents me from trying to 
// carry lots of stuff live across a call.
void Block::needed_for_next_call(Node *this_call, VectorSet &next_call, Block_Array &bbs) {
  // Find the next control-defining Node in this block
  Node* call = NULL;
  for (DUIterator_Fast imax, i = this_call->fast_outs(imax); i < imax; i++) {
    Node* m = this_call->fast_out(i);
    if( bbs[m->_idx] == this && // Local-block user
        m != this_call &&       // Not self-start node
        m->is_Call() )
      call = m;
      break;
  }
  if (call == NULL)  return;    // No next call (e.g., block end is near)
  // Set next-call for all inputs to this call
  set_next_call(call, next_call, bbs);
}

//------------------------------sched_call-------------------------------------
uint Block::sched_call( Matcher &m, Block_Array &bbs, uint node_cnt, Node_List &worklist, int *ready_cnt, MachCallNode *mcall, VectorSet &next_call ) {
  RegMask regs;

  // Schedule all the users of the call right now.  All the users are
  // projection Nodes, so they must be scheduled next to the call.
  // Collect all the defined registers.
  for (DUIterator_Fast imax, i = mcall->fast_outs(imax); i < imax; i++) {
    Node* n = mcall->fast_out(i);
    assert( n->Opcode()==Op_MachProj, "" );
    --ready_cnt[n->_idx];
    assert( !ready_cnt[n->_idx], "" );
    // Schedule next to call
    _nodes.map(node_cnt++, n);
    // Collect defined registers
    regs.OR(n->out_RegMask());
    // Check for scheduling the next control-definer
    if( n->bottom_type() == Type::CONTROL ) 
      // Warm up next pile of heuristic bits
      needed_for_next_call(n, next_call, bbs);

    // Children of projections are now all ready
    for (DUIterator_Fast jmax, j = n->fast_outs(jmax); j < jmax; j++) {
      Node* m = n->fast_out(j); // Get user
      if( bbs[m->_idx] != this ) continue;
      if( m->is_Phi() ) continue;
      if( !--ready_cnt[m->_idx] ) 
        worklist.push(m);
    }
  
  }

  // Act as if the call defines the Frame Pointer.
  // Certainly the FP is alive and well after the call.
  regs.Insert(m.c_frame_pointer());

  // Set all registers killed and not already defined by the call.
  uint r_cnt = mcall->tf()->range()->cnt();
  int op = mcall->ideal_Opcode();
  MachProjNode *proj = new (1) MachProjNode( mcall, r_cnt+1, RegMask::Empty, MachProjNode::fat_proj );
  bbs.map(proj->_idx,this);
  _nodes.insert(node_cnt++, proj);

  for( OptoReg::Name r = OptoReg::Name(0); r < _last_Mach_Reg; r=OptoReg::add(r,1) ) {
    if( !regs.Member(r) ) {     // Not already defined by the call  
      // Save-on-call register?
      if( (m._register_save_policy[r] == 'C') ||
          (m._register_save_policy[r] == 'A') ||
          ((m._register_save_policy[r] == 'E') &&
           (op == Op_CallRuntime     ||
            op == Op_CallNative      ||
            op == Op_CallInterpreter ||
            op == Op_CallLeaf        ||
            op == Op_CallLeafNoFP)) ) { 
        proj->_rout.Insert(r);
      }
    }
  }

  return node_cnt;
}


//------------------------------schedule_local---------------------------------
// Topological sort within a block.  Someday become a real scheduler.
bool Block::schedule_local(Matcher &matcher, Block_Array &bbs,int *ready_cnt, VectorSet &next_call, GrowableArray<uint> &node_latency) {
  // Already "sorted" are the block start Node (as the first entry), and
  // the block-ending Node and any trailing control projections.  We leave
  // these alone.  PhiNodes and ParmNodes are made to follow the block start
  // Node.  Everything else gets topo-sorted.

#ifndef PRODUCT
    if (TraceOptoPipelining) {
      tty->print("# before schedule_local\n");
      for (uint i = 0;i < _nodes.size();i++) {
        tty->print("# ");
        _nodes[i]->fast_dump();
      }
      tty->print("\n");
    }
#endif

  // RootNode is already sorted
  if( _nodes.size() == 1 ) return true;

  // Move PhiNodes and ParmNodes from 1 to cnt up to the start
  uint node_cnt = end_idx();
  uint phi_cnt = 1;
  uint i;
  for( i = 1; i<node_cnt; i++ ) { // Scan for Phi
    Node *n = _nodes[i];
    if( n->is_Phi() ||          // Found a PhiNode or ParmNode
        (n->is_Proj()  && n->in(0) == head()) ) {
      // Move guy at 'phi_cnt' to the end; makes a hole at phi_cnt
      _nodes.map(i,_nodes[phi_cnt]);
      _nodes.map(phi_cnt++,n);  // swap Phi/Parm up front
    } else {                    // All others
      // Count block-local inputs to 'n'
      uint cnt = n->len();      // Input count
      uint local = 0;
      for( uint j=0; j<cnt; j++ ) {
        Node *m = n->in(j);
        if( m && bbs[m->_idx] == this && !m->is_top() )
          local++;              // One more block-local input
      }
      ready_cnt[n->_idx] = local; // Count em up

      // A few node types require changing a required edge to a precedence edge
      // before allocation.
      MachNode *m = n->is_Mach();
      if( UseConcMarkSweepGC ) {
        if( m && m->ideal_Opcode() == Op_StoreCM ) {
          // Note: Required edges with an index greater than oper_input_base
          // are not supported by the allocator.
          // Note2: Can only depend on unmatched edge being last,
          // can not depend on its absolute position.
          Node *oop_store = n->in(n->req() - 1);
          n->del_req(n->req() - 1);
          n->add_prec(oop_store);
          assert(bbs[oop_store->_idx]->_dom_depth <= this->_dom_depth, "oop_store must dominate card-mark");
        }
      }
      if( m && m->ideal_Opcode() == Op_MemBarAcquire ) {
        Node *x = n->in(TypeFunc::Parms);
        n->del_req(TypeFunc::Parms);
        n->add_prec(x);
      }
    }
  }
  for(uint i2=i; i2<_nodes.size(); i2++ ) // Trailing guys get zapped count
    ready_cnt[_nodes[i2]->_idx] = 0;

  // All the prescheduled guys do not hold back internal nodes
  uint i3;
  for(i3 = 0; i3<phi_cnt; i3++ ) {  // For all pre-scheduled
    Node *n = _nodes[i3];       // Get pre-scheduled
    for (DUIterator_Fast jmax, j = n->fast_outs(jmax); j < jmax; j++) {
      Node* m = n->fast_out(j);
      if( bbs[m->_idx] ==this ) // Local-block user
        ready_cnt[m->_idx]--;   // Fix ready count
    }
  }

  // Make a worklist
  Node_List worklist;
  for(uint i4=i3; i4<node_cnt; i4++ ) {    // Put ready guys on worklist
    Node *m = _nodes[i4];    
    if( !ready_cnt[m->_idx] )   // Zero ready count?
      worklist.push(m);         // Then on to worklist!
  }

  // Warm up the 'next_call' heuristic bits
  needed_for_next_call(_nodes[0], next_call, bbs);

#ifndef PRODUCT
    if (TraceOptoPipelining) {
      for (uint j=0; j<_nodes.size(); j++) {
        Node     *n = _nodes[j];
        int     idx = n->_idx;
        tty->print("#   ready cnt:%3d  ", ready_cnt[idx]);
        tty->print("latency:%3d  ", node_latency.at_grow(idx));
        tty->print("%4d: %s\n", idx, n->Name());
      }
    }
#endif

  // Pull from worklist and schedule
  while( worklist.size() ) {    // Worklist is not ready

#ifndef PRODUCT
    uint before_size = worklist.size();

    if (TraceOptoPipelining && before_size > 1) {
      tty->print("#    before select:");
      for( uint i=0; i<worklist.size(); i++ ) { // Inspect entire worklist
        Node *n = worklist[i];      // Get Node on worklist
        tty->print(" %3d", n->_idx);
      }
      tty->print("\n");
    }
#endif

    // Select and pop a ready guy from worklist
    Node* n = select(worklist, bbs, ready_cnt, next_call, phi_cnt, node_latency);
    _nodes.map(phi_cnt++,n);    // Schedule him next
    MachNode *m = n->is_Mach();

#ifndef PRODUCT
    if (TraceOptoPipelining && before_size > 1) {
      tty->print("#  select %d: %s", n->_idx, n->Name());
      tty->print(", latency:%d", node_latency.at_grow(n->_idx));
      n->dump();
      tty->print("#    after select:");
      for( uint i=0; i<worklist.size(); i++ ) { // Inspect entire worklist
        Node *n = worklist[i];      // Get Node on worklist
        tty->print(" %4d", n->_idx);
      }
      tty->print("\n");
    }

#endif
    if( m ) {
      MachCallNode *mcall = m->is_MachCall();
      if( mcall ) {
        phi_cnt = sched_call(matcher, bbs, phi_cnt, worklist, ready_cnt, mcall, next_call);
        continue;
      }
    }
    // Children are now all ready
    for (DUIterator_Fast i5max, i5 = n->fast_outs(i5max); i5 < i5max; i5++) {
      Node* m = n->fast_out(i5); // Get user
      if( bbs[m->_idx] != this ) continue;
      if( m->is_Phi() ) continue;
      if( !--ready_cnt[m->_idx] ) 
        worklist.push(m);
    }
  }

  if( phi_cnt != end_idx() ) {
    // did not schedule all.  Retry, Bailout, or Die
    Compile* C = matcher.C;
    if (C->subsume_loads() == true && !C->failing()) {
      // Retry with subsume_loads == false
      // If this is the first failure, the sentinel string will "stick"
      // to the Compile object, and the C2Compiler will see it and retry.
      C->record_failure(C2Compiler::retry_no_subsuming_loads());
    }
    // assert( phi_cnt == end_idx(), "did not schedule all" );
    return false;
  }

#ifndef PRODUCT
  if (TraceOptoPipelining) {
    tty->print("# after schedule_local\n");
    for (uint i = 0;i < _nodes.size();i++) {
      tty->print("# ");
      _nodes[i]->fast_dump();
    }
    tty->print("\n");
  }
#endif


  return true;
}

//--------------------------catch_cleanup_fix_all_inputs-----------------------
static void catch_cleanup_fix_all_inputs(Node *use, Node *old_def, Node *new_def) {
  for (uint l = 0; l < use->len(); l++) {
    if (use->in(l) == old_def) {
      if (l < use->req()) {
        use->set_req(l, new_def);
      } else {
        use->rm_prec(l);
        use->add_prec(new_def);
        l--;
      }
    }
  }
}

//------------------------------catch_cleanup_find_cloned_def------------------
static Node *catch_cleanup_find_cloned_def(Block *use_blk, Node *def, Block *def_blk, Block_Array &bbs, int n_clone_idx) {
  assert( use_blk != def_blk, "Inter-block cleanup only");
  
  // The use is some block below the Catch.  Find and return the clone of the def
  // that dominates the use. If there is no clone in a dominating block, then
  // create a phi for the def in a dominating block.
  
  // Find which successor block dominates this use.  The successor
  // blocks must all be single-entry (from the Catch only; I will have
  // split blocks to make this so), hence they all dominate.
  while( use_blk->_dom_depth > def_blk->_dom_depth+1 )
    use_blk = use_blk->_idom;
  
  // Find the successor
  Node *fixup = NULL;

  uint j;
  for( j = 0; j < def_blk->_num_succs; j++ )
    if( use_blk == def_blk->_succs[j] ) 
      break;

  if( j == def_blk->_num_succs ) {
    // Block at same level in dom-tree is not a successor.  It needs a 
    // PhiNode, the PhiNode uses from the def and IT's uses need fixup.
    Node_Array inputs = new Node_List(Thread::current()->resource_area());
    for(uint k = 1; k < use_blk->num_preds(); k++) {
      inputs.map(k, catch_cleanup_find_cloned_def(bbs[use_blk->pred(k)->_idx], def, def_blk, bbs, n_clone_idx));
    }

    // Check to see if the use_blk already has an identical phi inserted.  
    // If it exists, it will be at the first position since all uses of a 
    // def are processed together.
    Node *phi = use_blk->_nodes[1];
    if( phi->is_Phi() ) {
      fixup = phi;
      for (uint k = 1; k < use_blk->num_preds(); k++) {
        if (phi->in(k) != inputs[k]) {
          // Not a match
          fixup = NULL;
          break;
        }
      }
    }

    // If an existing PhiNode was not found, make a new one.
    if (fixup == NULL) {
      Node *new_phi = PhiNode::make(use_blk->head(), def);
      use_blk->_nodes.insert(1, new_phi);
      bbs.map(new_phi->_idx, use_blk);
      for (uint k = 1; k < use_blk->num_preds(); k++) {
        new_phi->set_req(k, inputs[k]);
      }
      fixup = new_phi;
    }
    
  } else {
    // Found the use just below the Catch.  Make it use the clone.
    fixup = use_blk->_nodes[n_clone_idx];
  }

  return fixup;
}

//--------------------------catch_cleanup_intra_block--------------------------
// Fix all input edges in use that reference "def".  The use is in the same
// block as the def and both have been cloned in each successor block.
static void catch_cleanup_intra_block(Node *use, Node *def, Block *blk, int beg, int n_clone_idx) {

  // Both the use and def have been cloned. For each successor block,
  // get the clone of the use, and make its input the clone of the def
  // found in that block.

  uint use_idx = blk->find_node(use);
  uint offset_idx = use_idx - beg;
  for( uint k = 0; k < blk->_num_succs; k++ ) {
    // Get clone in each successor block
    Block *sb = blk->_succs[k];
    Node *clone = sb->_nodes[offset_idx+1];
    assert( clone->Opcode() == use->Opcode(), "" );

    // Make use-clone reference the def-clone
    catch_cleanup_fix_all_inputs(clone, def, sb->_nodes[n_clone_idx]);
  }
}

//------------------------------catch_cleanup_inter_block---------------------
// Fix all input edges in use that reference "def".  The use is in a different
// block than the def.
static void catch_cleanup_inter_block(Node *use, Block *use_blk, Node *def, Block *def_blk, Block_Array &bbs, int n_clone_idx) {
  if( !use_blk ) return;        // Can happen if the use is a precedence edge

  Node *new_def = catch_cleanup_find_cloned_def(use_blk, def, def_blk, bbs, n_clone_idx);
  catch_cleanup_fix_all_inputs(use, def, new_def);
}

//------------------------------call_catch_cleanup-----------------------------
// If we inserted any instructions between a Call and his CatchNode,
// clone the instructions on all paths below the Catch.
void Block::call_catch_cleanup(Block_Array &bbs) {

  // End of region to clone
  uint end = end_idx();
  if( !_nodes[end]->is_Catch() ) return;
  // Start of region to clone
  uint beg = end;
  while( _nodes[beg-1]->Opcode() != Op_MachProj || 
        !_nodes[beg-1]->in(0)->is_Call() ) {
    beg--;
    assert(beg > 0,"Catch cleanup walking beyond block boundary");
  }
  if( beg == end ) return;

  // Clone along all Catch output paths.  Clone area between the 'beg' and
  // 'end' indices.
  for( uint i = 0; i < _num_succs; i++ ) {
    Block *sb = _succs[i];
    // Clone the entire area; ignoring the edge fixup for now.
    for( uint j = end; j > beg; j-- ) {
      Node *clone = _nodes[j-1]->clone();
      sb->_nodes.insert( 1, clone );
      bbs.map(clone->_idx,sb);
    }
  }


  // Fixup edges.  Check the def-use info per cloned Node
  for(uint i2 = beg; i2 < end; i2++ ) {
    uint n_clone_idx = i2-beg+1; // Index of clone of n in each successor block
    Node *n = _nodes[i2];        // Node that got cloned
    // Need DU safe iterator because of edge manipulation in calls.
    Unique_Node_List *out = new Unique_Node_List(Thread::current()->resource_area());
    for (DUIterator_Fast j1max, j1 = n->fast_outs(j1max); j1 < j1max; j1++) {
      out->push(n->fast_out(j1));
    }
    uint max = out->size();
    for (uint j = 0; j < max; j++) {// For all users
      Node *use = out->pop();
      Block *buse = bbs[use->_idx];
      if( use->is_Phi() ) {
        for( uint k = 1; k < use->req(); k++ )
          if( use->in(k) == n ) {
            Node *fixup = catch_cleanup_find_cloned_def(bbs[buse->pred(k)->_idx], n, this, bbs, n_clone_idx);
            use->set_req(k, fixup);
          }
      } else {
        if (this == buse) {
          catch_cleanup_intra_block(use, n, this, beg, n_clone_idx);
        } else {
          catch_cleanup_inter_block(use, buse, n, this, bbs, n_clone_idx);
        }
      }
    } // End for all users

  } // End of for all Nodes in cloned area

  // Remove the now-dead cloned ops
  for(uint i3 = beg; i3 < end; i3++ ) {
    _nodes[beg]->disconnect_inputs(NULL);
    _nodes.remove(beg);
  }

  // If the successor blocks have a CreateEx node, move it back to the top
  for(uint i4 = 0; i4 < _num_succs; i4++ ) {
    Block *sb = _succs[i4];
    MachNode *cex = sb->_nodes[1+end-beg]->is_Mach();
    if( cex && cex->ideal_Opcode() == Op_CreateEx ) {
      sb->_nodes.remove(1+end-beg);
      sb->_nodes.insert(1,cex);
    }
  }
}




