#ifdef USE_PRAGMA_IDENT_HDR
#pragma ident "@(#)memoryService.cpp	1.18 04/05/14 14:35:20 JVM"
#endif
//
// Copyright 2004 Sun Microsystems, Inc.  All rights reserved.
// SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
//

# include "incls/_precompiled.incl"
# include "incls/_memoryService.cpp.incl"

GrowableArray<MemoryPool*>* MemoryService::_pools_list = 
  new (ResourceObj::C_HEAP) GrowableArray<MemoryPool*>(init_pools_list_size, true);
GrowableArray<MemoryManager*>* MemoryService::_managers_list = 
  new (ResourceObj::C_HEAP) GrowableArray<MemoryManager*>(init_managers_list_size, true);

GCMemoryManager* MemoryService::_minor_gc_manager = NULL;
GCMemoryManager* MemoryService::_major_gc_manager = NULL;
MemoryPool*      MemoryService::_code_heap_pool   = NULL;

class GcThreadCountClosure: public ThreadClosure {
 private:
  int _count;
 public:
  GcThreadCountClosure() : _count(0) {};
  void do_thread(Thread* thread);
  int count() { return _count; }
};

void GcThreadCountClosure::do_thread(Thread* thread) {
  _count++;
}

void MemoryService::set_universe_heap(CollectedHeap* heap) {
  CollectedHeap::Name kind = heap->kind();
  switch (kind) {
    case CollectedHeap::GenCollectedHeap : {
      add_gen_collected_heap_info(GenCollectedHeap::heap());
      break;
    }
    case CollectedHeap::ParallelScavengeHeap : {
      add_parallel_scavenge_heap_info(ParallelScavengeHeap::heap());
      break;
    default:
      guarantee(false, "Not recognized kind of heap");
    }
  }

  // set the GC thread count
  GcThreadCountClosure gctcc;
  heap->gc_threads_do(&gctcc);
  int count = gctcc.count();
  if (count > 0) {
    _minor_gc_manager->set_num_gc_threads(count);
    _major_gc_manager->set_num_gc_threads(count);
  }

  // All memory pools and memory managers are initialized.
  //
  _minor_gc_manager->initialize_gc_stat_info();
  _major_gc_manager->initialize_gc_stat_info();
}

// Add memory pools for GenCollectedHeap
// This function currently only supports two generations collected heap.
// The collector for GenCollectedHeap will have two memory managers.
void MemoryService::add_gen_collected_heap_info(GenCollectedHeap* heap) {
  CollectorPolicy* policy = heap->collector_policy();

  assert(policy->is_two_generation_policy(), "Only support two generations");
  guarantee(heap->n_gens() == 2, "Only support two-generation heap");

  if (policy->is_mark_sweep_policy()) {
    _minor_gc_manager = MemoryManager::get_copy_memory_manager();
    _major_gc_manager = MemoryManager::get_msc_memory_manager();
  } else if (policy->is_train_policy()) {
    _minor_gc_manager = MemoryManager::get_copy_memory_manager();
    _major_gc_manager = MemoryManager::get_train_memory_manager();
  } else if (policy->is_concurrent_mark_sweep_policy()) {
    GenerationSpec** specs = policy->generations();
    Generation::Name kind = specs[0]->name();
    switch (kind) {
      case Generation::DefNew:
        _minor_gc_manager = MemoryManager::get_copy_memory_manager();
        break;
      case Generation::ParNew: 
        _minor_gc_manager = MemoryManager::get_parnew_memory_manager();
        break;
      default:
        guarantee(false, "Unrecognized generation spec");
        break;
    }
    _major_gc_manager = MemoryManager::get_cms_memory_manager();
  } 
  _managers_list->append(_minor_gc_manager);
  _managers_list->append(_major_gc_manager);

  add_generation_memory_pool(heap->get_gen(minor), _major_gc_manager, _minor_gc_manager);
  add_generation_memory_pool(heap->get_gen(major), _major_gc_manager);

  PermGen::Name name = policy->permanent_generation()->name();
  switch (name) {
    case PermGen::MarkSweepCompact: {
      CompactingPermGenGen* perm_gen = (CompactingPermGenGen*) heap->perm_gen();
      add_compact_perm_gen_memory_pool(perm_gen, _major_gc_manager);
      break;
    }
    case PermGen::ConcurrentMarkSweep: {
      CMSPermGenGen* cms_gen = (CMSPermGenGen*) heap->perm_gen();
      add_cms_perm_gen_memory_pool(cms_gen, _major_gc_manager);
      break;
    }
    default:
      guarantee(false, "Unrecognized perm generation");
        break;
  }
}

// Add memory pools for ParallelScavengeHeap 
// This function currently only supports two generations collected heap.
// The collector for ParallelScavengeHeap will have two memory managers.
void MemoryService::add_parallel_scavenge_heap_info(ParallelScavengeHeap* heap) {
  // Two managers to keep statistics about _minor_gc_manager and _major_gc_manager GC.
  _minor_gc_manager = MemoryManager::get_psScavenge_memory_manager();
  _major_gc_manager = MemoryManager::get_psMarkSweep_memory_manager();
  _managers_list->append(_minor_gc_manager);
  _managers_list->append(_major_gc_manager);

  add_psYoung_memory_pool(heap->young_gen(), _major_gc_manager, _minor_gc_manager);
  add_psOld_memory_pool(heap->old_gen(), _major_gc_manager);
  add_psPerm_memory_pool(heap->perm_gen(), _major_gc_manager);
}

MemoryPool* MemoryService::add_gen(Generation* gen, 
                                   const char* name, 
                                   bool is_heap,
                                   bool support_usage_threshold) {
 
  MemoryPool::PoolType type = (is_heap ? MemoryPool::Heap : MemoryPool::NonHeap);
  GenerationPool* pool = new GenerationPool(gen, name, type, support_usage_threshold);
  _pools_list->append(pool);
  return (MemoryPool*) pool;
}

MemoryPool* MemoryService::add_space(ContiguousSpace* space,
                                     const char* name,   
                                     bool is_heap,
                                     size_t max_size,
                                     bool support_usage_threshold) {
  MemoryPool::PoolType type = (is_heap ? MemoryPool::Heap : MemoryPool::NonHeap);
  ContiguousSpacePool* pool = new ContiguousSpacePool(space, name, type, max_size, support_usage_threshold);
 
  _pools_list->append(pool);
  return (MemoryPool*) pool;
}

MemoryPool* MemoryService::add_survivor_spaces(DefNewGeneration* gen,
                                               const char* name,   
                                               bool is_heap,
                                               size_t max_size,
                                               bool support_usage_threshold) {
  MemoryPool::PoolType type = (is_heap ? MemoryPool::Heap : MemoryPool::NonHeap);
  SurvivorContiguousSpacePool* pool = new SurvivorContiguousSpacePool(gen, name, type, max_size, support_usage_threshold);
 
  _pools_list->append(pool);
  return (MemoryPool*) pool;
}

MemoryPool* MemoryService::add_cms_space(CompactibleFreeListSpace* space,
                                         const char* name,
                                         bool is_heap,
                                         size_t max_size,
                                         bool support_usage_threshold) {
  MemoryPool::PoolType type = (is_heap ? MemoryPool::Heap : MemoryPool::NonHeap);
  CompactibleFreeListSpacePool* pool = new CompactibleFreeListSpacePool(space, name, type, max_size, support_usage_threshold);
  _pools_list->append(pool);
  return (MemoryPool*) pool;
}

// Add memory pool(s) for one generation
void MemoryService::add_generation_memory_pool(Generation* gen, 
                                               MemoryManager* major_mgr,
                                               MemoryManager* minor_mgr) {
  Generation::Name kind = gen->kind();
  int index = _pools_list->length();

  switch (kind) {
    case Generation::DefNew: {
      assert(major_mgr != NULL && minor_mgr != NULL, "Should have two managers");
      DefNewGeneration* young_gen = (DefNewGeneration*) gen;
      // Add a memory pool for each space and young gen doesn't 
      // support low memory detection as it is expected to get filled up.
      MemoryPool* eden = add_space(young_gen->eden(),
                                   "Eden Space",
                                   true, /* is_heap */
                                   young_gen->max_eden_size(),
                                   false /* support_usage_threshold */);
      MemoryPool* survivor = add_survivor_spaces(young_gen, 
                                                 "Survivor Space", 
                                                 true, /* is_heap */
                                                 young_gen->max_survivor_size(),
                                                 false /* support_usage_threshold */);
      break;
    }

    case Generation::ParNew: {
      assert(major_mgr != NULL && minor_mgr != NULL, "Should have two managers");
      // Add a memory pool for each space and young gen doesn't 
      // support low memory detection as it is expected to get filled up.
      ParNewGeneration* parnew_gen = (ParNewGeneration*) gen;
      MemoryPool* eden = add_space(parnew_gen->eden(), 
                                   "Par Eden Space", 
                                   true /* is_heap */, 
                                   parnew_gen->max_eden_size(),
                                   false /* support_usage_threshold */);
      MemoryPool* survivor = add_survivor_spaces(parnew_gen,
                                                 "Par Survivor Space",
                                                 true, /* is_heap */
                                                 parnew_gen->max_survivor_size(),
                                                 false /* support_usage_threshold */);
      
      break;
    }

    case Generation::MarkSweepCompact: {
      assert(major_mgr != NULL && minor_mgr == NULL, "Should have only one manager");
      add_gen(gen,
              "Tenured Gen",
              true, /* is_heap */
              true  /* support_usage_threshold */);
      break;
    }

    case Generation::TrainGen: {
      assert(major_mgr != NULL && minor_mgr == NULL, "Should have only one manager");
      // Create one memory pool for the train generation.
      // We could probably define one memory pool per train
      // but we keep it simple since the train collector will
      // be going away.
      add_gen(gen,
              "Train Gen",
              true, /* is_heap */
              true  /* support_usage_threshold */);
      break;
    }

    case Generation::ConcurrentMarkSweep: {
      assert(major_mgr != NULL && minor_mgr == NULL, "Should have only one manager");
      ConcurrentMarkSweepGeneration* cms = (ConcurrentMarkSweepGeneration*) gen;
      MemoryPool* pool = add_cms_space(cms->cmsSpace(),
                                       "CMS Old Gen",
                                       true, /* is_heap */
                                       cms->reserved().byte_size(),
                                       true  /* support_usage_threshold */);
      break;
    }

    default:
      assert(false, "should not reach here");
      // no memory pool added for others
      break;
  }

  assert(major_mgr != NULL, "Should have at least one manager");
  // Link managers and the memory pools together
  for (int i = index; i < _pools_list->length(); i++) {
    MemoryPool* pool = _pools_list->at(i);
    major_mgr->add_pool(pool);
    if (minor_mgr != NULL) {
      minor_mgr->add_pool(pool);
    }
  }
}

void MemoryService::add_compact_perm_gen_memory_pool(CompactingPermGenGen* perm_gen,
                                                     MemoryManager* mgr) {
  PermanentGenerationSpec* spec = perm_gen->spec();
  size_t max_size = spec->max_size() - spec->read_only_size() - spec->read_write_size();
  MemoryPool* pool = add_space(perm_gen->unshared_space(),
                               "Perm Gen", 
                                false, /* is_heap */
                                max_size,
                                true   /* support_usage_threshold */);
  mgr->add_pool(pool);
  if (UseSharedSpaces) {
    pool = add_space(perm_gen->ro_space(),
                     "Perm Gen [shared-ro]",
                     false, /* is_heap */
                     spec->read_only_size(),
                     true   /* support_usage_threshold */);
    mgr->add_pool(pool);

    pool = add_space(perm_gen->rw_space(),
                     "Perm Gen [shared-rw]",
                     false, /* is_heap */
                     spec->read_write_size(),
                     true   /* support_usage_threshold */);
    mgr->add_pool(pool);
  }
}

void MemoryService::add_cms_perm_gen_memory_pool(CMSPermGenGen* cms_gen,
                                                 MemoryManager* mgr) {

  MemoryPool* pool = add_cms_space(cms_gen->cmsSpace(),
                                   "CMS Perm Gen",
                                   false, /* is_heap */
                                   cms_gen->reserved().byte_size(),
                                   true   /* support_usage_threshold */);
  mgr->add_pool(pool);
}

void MemoryService::add_psYoung_memory_pool(PSYoungGen* gen, MemoryManager* major_mgr, MemoryManager* minor_mgr) {
  assert(major_mgr != NULL && minor_mgr != NULL, "Should have two managers");

  // Add a memory pool for each space and young gen doesn't 
  // support low memory detection as it is expected to get filled up.
  EdenMutableSpacePool* eden = new EdenMutableSpacePool(gen,
                                                        gen->eden_space(), 
                                                        "PS Eden Space", 
                                                        MemoryPool::Heap,
                                                        false /* support_usage_threshold */);

  SurvivorMutableSpacePool* survivor = new SurvivorMutableSpacePool(gen,
                                                                    "PS Survivor Space", 
                                                                    MemoryPool::Heap,
                                                                    false /* support_usage_threshold */);

  major_mgr->add_pool(eden);
  major_mgr->add_pool(survivor);
  minor_mgr->add_pool(eden);
  minor_mgr->add_pool(survivor);
  _pools_list->append(eden);
  _pools_list->append(survivor);
}

void MemoryService::add_psOld_memory_pool(PSOldGen* gen, MemoryManager* mgr) {
  PSGenerationPool* old_gen = new PSGenerationPool(gen, 
                                                   "PS Old Gen",
                                                   MemoryPool::Heap, 
                                                   true /* support_usage_threshold */);
  mgr->add_pool(old_gen);
  _pools_list->append(old_gen);
}

void MemoryService::add_psPerm_memory_pool(PSPermGen* gen, MemoryManager* mgr) {
  PSGenerationPool* perm_gen = new PSGenerationPool(gen, 
                                                    "PS Perm Gen", 
                                                    MemoryPool::NonHeap,
                                                    true /* support_usage_threshold */);
  mgr->add_pool(perm_gen);
  _pools_list->append(perm_gen);
}

void MemoryService::add_code_heap_memory_pool(CodeHeap* heap) {
  _code_heap_pool = new CodeHeapPool(heap,
                                     "Code Cache",
                                     true /* support_usage_threshold */);
  MemoryManager* mgr = MemoryManager::get_code_cache_memory_manager();
  mgr->add_pool(_code_heap_pool);

  _pools_list->append(_code_heap_pool);
  _managers_list->append(mgr);
}

MemoryManager* MemoryService::get_memory_manager(instanceHandle mh) {
  for (int i = 0; i < _managers_list->length(); i++) {
    MemoryManager* mgr = _managers_list->at(i);
    if (mgr->is_manager(mh)) {
      return mgr;
    }
  }
  return NULL;
}

MemoryPool* MemoryService::get_memory_pool(instanceHandle ph) {
  for (int i = 0; i < _pools_list->length(); i++) {
    MemoryPool* pool = _pools_list->at(i);
    if (pool->is_pool(ph)) {
      return pool;
    }
  }
  return NULL;
}

void MemoryService::track_memory_usage() {
  // Track the peak memory usage
  for (int i = 0; i < _pools_list->length(); i++) {
    MemoryPool* pool = _pools_list->at(i);
    pool->record_peak_memory_usage();
  }

  // Detect low memory
  LowMemoryDetector::detect_low_memory();
}

void MemoryService::track_memory_pool_usage(MemoryPool* pool) {
  // Track the peak memory usage
  pool->record_peak_memory_usage();
  
  // Detect low memory
  if (LowMemoryDetector::is_enabled(pool)) {
    LowMemoryDetector::detect_low_memory(pool);
  }
}

void MemoryService::gc_begin(bool fullGC) {
  GCMemoryManager* mgr; 
  if (fullGC) {
    mgr = _major_gc_manager;
  } else {
    mgr = _minor_gc_manager;
  }
  assert(mgr->is_gc_memory_manager(), "Sanity check");
  mgr->gc_begin();

  // Track the peak memory usage when GC begins
  for (int i = 0; i < _pools_list->length(); i++) {
    MemoryPool* pool = _pools_list->at(i);
    pool->record_peak_memory_usage();
  }
}

void MemoryService::gc_end(bool fullGC) {
  GCMemoryManager* mgr; 
  if (fullGC) {
    mgr = (GCMemoryManager*) _major_gc_manager;
  } else {
    mgr = (GCMemoryManager*) _minor_gc_manager;
  }
  assert(mgr->is_gc_memory_manager(), "Sanity check");

  // register the GC end statistics and memory usage
  mgr->gc_end();
}

void MemoryService::oops_do(OopClosure* f) {
  int i;

  for (i = 0; i < _pools_list->length(); i++) {
    MemoryPool* pool = _pools_list->at(i);
    pool->oops_do(f);
  }
  for (i = 0; i < _managers_list->length(); i++) {
    MemoryManager* mgr = _managers_list->at(i);
    mgr->oops_do(f);
  }
}

bool MemoryService::set_verbose(bool verbose) {
  MutexLocker m(Management_lock);
  bool prev = PrintGC;
  PrintGC = verbose;
  ClassLoadingService::reset_trace_class_unloading(); 

  return prev;
}

Handle MemoryService::create_MemoryUsage_obj(MemoryUsage usage, TRAPS) {
  klassOop k = Management::java_lang_management_MemoryUsage_klass(CHECK_0);
  instanceKlassHandle ik(THREAD, k);

  instanceHandle obj = ik->allocate_instance_handle(CHECK_0);

  JavaValue result(T_VOID);
  JavaCallArguments args(10);
  args.push_oop(obj);                         // receiver
  args.push_long(usage.init_size_as_jlong()); // Argument 1
  args.push_long(usage.used_as_jlong());      // Argument 2
  args.push_long(usage.committed_as_jlong()); // Argument 3
  args.push_long(usage.max_size_as_jlong());  // Argument 4

  JavaCalls::call_special(&result,
                          ik,
                          vmSymbolHandles::object_initializer_name(),
                          vmSymbolHandles::long_long_long_long_void_signature(),
                          &args,
                          CHECK_0);
  return obj;
}

TraceMemoryManagerStats::TraceMemoryManagerStats(bool fullGC) {
  _fullGC = fullGC;
  MemoryService::gc_begin(_fullGC);
}

TraceMemoryManagerStats::~TraceMemoryManagerStats() {
  MemoryService::gc_end(_fullGC);
}
