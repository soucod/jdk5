#ifdef USE_PRAGMA_IDENT_SRC
#pragma ident "@(#)fieldDescriptor.cpp	1.49 03/12/23 16:43:38 JVM"
#endif
/*
 * Copyright 2004 Sun Microsystems, Inc.  All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL.  Use is subject to license terms.
 */

# include "incls/_precompiled.incl"
#include "incls/_fieldDescriptor.cpp.incl"


oop fieldDescriptor::loader() const {
  return instanceKlass::cast(_cp->pool_holder())->class_loader(); 
}
                   
typeArrayOop fieldDescriptor::annotations() const {
  instanceKlass* ik = instanceKlass::cast(field_holder());
  objArrayOop md = ik->fields_annotations();
  if (md == NULL)
    return NULL;
  assert((index() % instanceKlass::next_offset) == 0, "");
  return typeArrayOop(md->obj_at(index() / instanceKlass::next_offset));
}

constantTag fieldDescriptor::initial_value_tag() const {
  return constants()->tag_at(_initial_value_index);
}

jint fieldDescriptor::int_initial_value() const {
  return constants()->int_at(_initial_value_index);
}

jlong fieldDescriptor::long_initial_value() const {
  return constants()->long_at(_initial_value_index);
}

jfloat fieldDescriptor::float_initial_value() const {
  return constants()->float_at(_initial_value_index);
}

jdouble fieldDescriptor::double_initial_value() const {
  return constants()->double_at(_initial_value_index);
}

oop fieldDescriptor::string_initial_value(TRAPS) const {
  return constants()->string_at(_initial_value_index, CHECK_0);
}

void fieldDescriptor::initialize(klassOop k, int index) {    
  instanceKlass* ik = instanceKlass::cast(k);
  _cp = ik->constants();
  typeArrayOop fields = ik->fields();

  assert(fields->length() % instanceKlass::next_offset == 0, "Illegal size of field array");
  assert(fields->length() >= index + instanceKlass::next_offset, "Illegal size of field array");

  _access_flags.set_field_flags(fields->ushort_at(index + instanceKlass::access_flags_offset));
  _name_index = fields->ushort_at(index + instanceKlass::name_index_offset);
  _signature_index = fields->ushort_at(index + instanceKlass::signature_index_offset);
  _initial_value_index = fields->ushort_at(index + instanceKlass::initval_index_offset);
  guarantee(_name_index != 0 && _signature_index != 0, "bad constant pool index for fieldDescriptor");
  _offset = ik->offset_from_fields( index );
  _generic_signature_index = fields->ushort_at(index + instanceKlass::generic_signature_offset);
  _index = index;
}

#ifndef PRODUCT

void fieldDescriptor::print_on(outputStream* st) const {
  _access_flags.print_on(st);
  constants()->symbol_at(_name_index)->print_value_on(st);
  st->print(" ");
  constants()->symbol_at(_signature_index)->print_value_on(st);
  st->print(" @%d ", offset());
  if (WizardMode && has_initial_value()) {
    st->print("(initval ");
    constantTag t = initial_value_tag();
    if (t.is_int()) {
      st->print("int %d)", int_initial_value());
    } else if (t.is_long()){
      st->print_jlong(long_initial_value());
    } else if (t.is_float()){
      st->print("float %f)", float_initial_value());
    } else if (t.is_double()){
      st->print("double %lf)", double_initial_value());
    }
  }
}

void fieldDescriptor::print_on_for(outputStream* st, oop obj) {
  print_on(st);
  BasicType ft = field_type();
  jint as_int;
  switch (ft) {
    case T_BYTE:
      as_int = (jint)obj->byte_field(offset());
      st->print(" %d", obj->byte_field(offset()));
      break;
    case T_CHAR:
      {
        jchar c = obj->char_field(offset());
	as_int = c;
        st->print(" %c %d", isprint(c) ? c : ' ', c);
      }
      break;
    case T_DOUBLE:
      st->print(" %lf", obj->double_field(offset()));
      break;
    case T_FLOAT:
      as_int = obj->int_field(offset());
      st->print(" %f", obj->float_field(offset()));
      break;
    case T_INT:
      st->print(" %d", obj->int_field(offset()));
      break;
    case T_LONG:
      st->print(" ");
      st->print_jlong(obj->long_field(offset()));
      break;
    case T_SHORT:
      as_int = obj->short_field(offset());
      st->print(" %d", obj->short_field(offset()));
      break;
    case T_BOOLEAN:
      as_int = obj->bool_field(offset());
      st->print(" %s", obj->bool_field(offset()) ? "true" : "false");
      break;
    case T_ARRAY:
      st->print(" ");
      as_int = obj->int_field(offset());
      obj->obj_field(offset())->print_value_on(st);
      break;
    case T_OBJECT:
      st->print(" ");
      as_int = obj->int_field(offset());
      obj->obj_field(offset())->print_value_on(st);
      break;
    default:
      ShouldNotReachHere();
      break;
  }
  // Print a hint as to the underlying integer representation. This can be wrong for
  // pointers on an LP64 machine
  if (ft == T_LONG || ft == T_DOUBLE) {
    st->print(" (%x %x)", obj->int_field(offset()), obj->int_field(offset()+sizeof(jint)));
  } else {
    st->print(" (%x)", as_int);
  }
}

#endif /* PRODUCT */
