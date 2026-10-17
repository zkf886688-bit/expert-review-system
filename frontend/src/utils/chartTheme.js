export const chartColors = {

  primary: '#5c6cff',

  green: '#10b981',

  amber: '#f59e0b',

  red: '#ef4444',

  purple: '#8b5cf6',

  slate: '#6b7289',

  cyan: '#22d3ee'

}



export const piePalette = [

  chartColors.primary,

  chartColors.cyan,

  chartColors.green,

  chartColors.amber,

  chartColors.purple,

  chartColors.red,

  '#818cf8',

  chartColors.slate

]



function emptyGraphic(text = '暂无数据') {

  return {

    type: 'text',

    left: 'center',

    top: 'middle',

    style: { text, fill: '#9aa3b5', fontSize: 13 }

  }

}



export function pieOption(dataObj, extra = {}) {

  const data = Object.entries(dataObj || {}).map(([name, value]) => ({ name, value }))

  return {

    color: piePalette,

    tooltip: { trigger: 'item', borderWidth: 0, boxShadow: '0 8px 24px rgba(22, 24, 42, 0.08)' },

    graphic: data.length ? undefined : emptyGraphic(),

    series: [{

      type: 'pie',

      radius: ['44%', '70%'],

      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },

      label: { color: '#6b7289', fontSize: 12 },

      data,

      ...extra

    }]

  }

}



export function barOption(labels, values, color = chartColors.primary) {

  const hasData = (labels || []).length > 0

  return {

    color: [color],

    graphic: hasData ? undefined : emptyGraphic(),

    tooltip: { trigger: 'axis' },

    grid: { left: 48, right: 16, top: 24, bottom: 48 },

    xAxis: {

      type: 'category',

      data: labels,

      axisLabel: { rotate: labels.length > 6 ? 30 : 0, color: '#6b7289' },

      axisLine: { lineStyle: { color: '#e8eaf3' } }

    },

    yAxis: {

      type: 'value',

      minInterval: 1,

      splitLine: { lineStyle: { color: '#f0f1f6' } },

      axisLabel: { color: '#6b7289' }

    },

    series: [{

      type: 'bar',

      data: values,

      barMaxWidth: 32,

      itemStyle: { borderRadius: [8, 8, 0, 0] }

    }]

  }

}



export function lineOption(labels, values, color = chartColors.primary) {

  const hasData = (labels || []).length > 0

  return {

    color: [color],

    graphic: hasData ? undefined : emptyGraphic(),

    tooltip: { trigger: 'axis' },

    grid: { left: 48, right: 16, top: 24, bottom: 32 },

    xAxis: {

      type: 'category',

      data: labels,

      axisLine: { lineStyle: { color: '#e8eaf3' } },

      axisLabel: { color: '#6b7289' }

    },

    yAxis: {

      type: 'value',

      minInterval: 1,

      splitLine: { lineStyle: { color: '#f0f1f6' } },

      axisLabel: { color: '#6b7289' }

    },

    series: [{

      type: 'line',

      data: values,

      smooth: true,

      symbolSize: 8,

      lineStyle: { width: 3 },

      areaStyle: {

        color: {

          type: 'linear',

          x: 0, y: 0, x2: 0, y2: 1,

          colorStops: [

            { offset: 0, color: 'rgba(92, 108, 255, 0.2)' },

            { offset: 1, color: 'rgba(92, 108, 255, 0)' }

          ]

        }

      }

    }]

  }

}

