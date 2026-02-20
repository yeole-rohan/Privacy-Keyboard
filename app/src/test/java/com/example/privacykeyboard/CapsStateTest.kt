package com.example.privacykeyboard

import com.example.privacykeyboard.model.CapsState
import com.example.privacykeyboard.util.nextCapsState
import com.example.privacykeyboard.util.shouldAutoOffAfterKey
import org.junit.Assert.*
import org.junit.Test

class CapsStateTest {

    // -----------------------------------------------------------------------
    // CapsState transitions
    // -----------------------------------------------------------------------

    @Test
    fun `nextCapsState cycles OFF to SHIFT`() {
        assertEquals(CapsState.SHIFT, nextCapsState(CapsState.OFF))
    }

    @Test
    fun `nextCapsState cycles SHIFT to CAPS_LOCK`() {
        assertEquals(CapsState.CAPS_LOCK, nextCapsState(CapsState.SHIFT))
    }

    @Test
    fun `nextCapsState cycles CAPS_LOCK back to OFF`() {
        assertEquals(CapsState.OFF, nextCapsState(CapsState.CAPS_LOCK))
    }

    @Test
    fun `full three-tap cycle returns to original state`() {
        var state = CapsState.OFF
        repeat(3) { state = nextCapsState(state) }
        assertEquals(CapsState.OFF, state)
    }

    // -----------------------------------------------------------------------
    // Auto-off after key press
    // -----------------------------------------------------------------------

    @Test
    fun `shouldAutoOffAfterKey is true only for SHIFT`() {
        assertTrue(shouldAutoOffAfterKey(CapsState.SHIFT))
        assertFalse(shouldAutoOffAfterKey(CapsState.OFF))
        assertFalse(shouldAutoOffAfterKey(CapsState.CAPS_LOCK))
    }

    @Test
    fun `CAPS_LOCK does not auto-off after typing a letter`() {
        var state = CapsState.CAPS_LOCK
        repeat(5) {
            if (shouldAutoOffAfterKey(state)) state = CapsState.OFF
        }
        assertEquals(CapsState.CAPS_LOCK, state)
    }

    @Test
    fun `SHIFT auto-offs after exactly one key press`() {
        var state = CapsState.SHIFT
        if (shouldAutoOffAfterKey(state)) state = CapsState.OFF
        assertEquals(CapsState.OFF, state)
    }
}
