package com.labeq.device

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class ParameterSetter(val identifier: String)

@Target(AnnotationTarget.FUNCTION)
@MustBeDocumented
annotation class ParameterGetter(val identifier: String)
