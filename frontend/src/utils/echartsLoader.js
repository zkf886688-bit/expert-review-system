let echartsModule = null

export async function loadEcharts() {
  if (!echartsModule) {
    const mod = await import('echarts')
    echartsModule = mod.default ?? mod
  }
  return echartsModule
}
