package com.yasuda.derbis.architecture.mobius

data class DerbisNext<Model, Effect>(
    val model: Model,
    val effects: Set<Effect> = emptySet()
) {
    companion object {
        fun <Model, Effect> next(model: Model): DerbisNext<Model, Effect> = DerbisNext(model)

        fun <Model, Effect> dispatch(model: Model, vararg effects: Effect): DerbisNext<Model, Effect> =
            DerbisNext(model, effects.toSet())
    }
}

fun interface DerbisInit<Model, Effect> {
    fun init(model: Model): DerbisNext<Model, Effect>
}

fun interface DerbisUpdate<Model, Event, Effect> {
    fun update(model: Model, event: Event): DerbisNext<Model, Effect>
}

fun interface DerbisViewMapper<Model, ViewData> {
    fun map(model: Model): ViewData
}
