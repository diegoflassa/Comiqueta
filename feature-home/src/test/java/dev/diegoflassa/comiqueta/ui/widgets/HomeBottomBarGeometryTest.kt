package dev.diegoflassa.comiqueta.ui.widgets

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeBottomBarGeometryTest {

    @Test
    fun referenceWidth_matchesDesignContract() {
        val geometry = HomeBottomBarGeometry.fromWidth(360.dp)

        assertThat(geometry.scale).isEqualTo(1f)
        assertThat(geometry.bodyHeight).isEqualTo(56.dp)
        assertThat(geometry.circleDiameter).isEqualTo(52.dp)
        assertThat(geometry.centerX).isEqualTo(180.dp)
        assertThat(geometry.centerYFromBodyTop).isEqualTo(12.dp)
        assertThat(geometry.protrusion).isEqualTo(14.dp)
        assertThat(geometry.occupiedHeight).isEqualTo(70.dp)
    }

    @Test
    fun representativeWidths_scaleBodyHeightUniformly() {
        assertBodyHeight(320.dp, 56f * 320f / 360f)
        assertBodyHeight(360.dp, 56f)
        assertBodyHeight(411.dp, 56f * 411f / 360f)
        assertBodyHeight(600.dp, 56f * 600f / 360f)
        assertBodyHeight(800.dp, 56f * 800f / 360f)
    }

    @Test
    fun circleAndProtrusion_shareTheBodyScale() {
        val geometry = HomeBottomBarGeometry.fromWidth(411.dp)
        val s = 411f / 360f

        assertThat(geometry.circleDiameter.value).isWithin(0.001f).of(52f * s)
        assertThat(geometry.centerYFromBodyTop.value).isWithin(0.001f).of(12f * s)
        assertThat(geometry.protrusion.value).isWithin(0.001f).of(14f * s)
        assertThat(geometry.occupiedHeight.value)
            .isWithin(0.001f)
            .of(geometry.bodyHeight.value + geometry.protrusion.value)
    }

    private fun assertBodyHeight(width: Dp, expectedDp: Float) {
        val geometry = HomeBottomBarGeometry.fromWidth(width)
        assertThat(geometry.scale).isWithin(0.0001f).of(width.value / 360f)
        assertThat(geometry.bodyHeight.value).isWithin(0.001f).of(expectedDp)
    }
}
