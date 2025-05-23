package com.labeq.device.generic.oscilloscope

import kotlinx.serialization.Serializable

/**
 * Represents a trigger configuration for an oscilloscope.
 *
 * @property level The trigger level, indicating the voltage at which the oscilloscope's trigger fires.
 * @property coupling The coupling mode, specifying how the input signal is filtered or processed
 *                    before being evaluated by the trigger. Possible values are defined in
 *                    [OscilloscopeTriggerCoupling].
 * @property source The source channel index from which the trigger signal originates.
 */
@Serializable
data class OscilloscopeTrigger(val level: Double, val coupling: OscilloscopeTriggerCoupling, val source: Int)