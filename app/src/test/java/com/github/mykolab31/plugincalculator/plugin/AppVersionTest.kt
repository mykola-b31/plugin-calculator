package com.github.mykolab31.plugincalculator.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppVersionTest {

    @Test
    fun `equal versions are considered at least`() {
        assertEquals(true, AppVersion.isAtLeast("1.0.0", "1.0.0"))
    }

    @Test
    fun `different segment counts compare correctly when equal`() {
        assertEquals(true, AppVersion.isAtLeast("1.0", "1.0.0"))
        assertEquals(true, AppVersion.isAtLeast("1.0.0", "1.0"))
    }

    @Test
    fun `higher version satisfies lower minimum`() {
        assertEquals(true, AppVersion.isAtLeast("2.1.0", "1.9.9"))
    }

    @Test
    fun `version with more segments compares correctly numerically`() {
        assertEquals(true, AppVersion.isAtLeast("1.10.0", "1.9.0"))
    }

    @Test
    fun `lower version does not satisfy higher minimum`() {
        assertEquals(false, AppVersion.isAtLeast("1.0.0", "1.1.0"))
    }

    @Test
    fun `malformed version returns null`() {
        assertNull(AppVersion.isAtLeast("abc", "1.0.0"))
        assertNull(AppVersion.isAtLeast("1.0.0", "not-a-version"))
    }
}