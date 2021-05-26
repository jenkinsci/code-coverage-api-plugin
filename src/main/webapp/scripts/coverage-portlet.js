
var CoveragePortletChart = function (id) {

    const chartDom = document.getElementById(id);
    const portletChart = echarts.init(chartDom);
    const data = JSON.parse(chartDom.getAttribute('data'));

    const covered = data.covered;
    const missed = data.missed;

    const option = {
        tooltip: {
            trigger: 'axis',
            axisPointer: {
                type: 'shadow'
            },
            formatter: function (obj) {
                if (Array.isArray(obj)) {
                    if (obj.length === 2) {
                        return '<div style="text-align: left"><b>' + obj[0].name + '</b><br/>'
                            + obj[0].marker + ' ' + obj[0].seriesName + '&nbsp;&nbsp;' + covered[obj[0].dataIndex] + '<br/>'
                            + obj[1].marker + ' ' + obj[1].seriesName + '&nbsp;&nbsp;&nbsp;&nbsp;' + missed[obj[1].dataIndex] + '</div>';

                    } else if (obj.length === 1) {
                        return '<div style="text-align: left"><b>' + obj[0].name + '</b><br/>'
                            + obj[0].marker + ' ' + obj[0].seriesName + '&nbsp;&nbsp;'
                            + (obj[0].seriesName === 'Covered' ? covered[obj[0].dataIndex] : missed[obj[0].dataIndex]) + '</div>';
                    }
                }
            }
        },
        legend: {
            data: ['Covered', 'Missed']
        },
        grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
        },
        xAxis: {
            type: 'value',
            name: 'in %',
        },
        yAxis: [{
            type: 'category',
            data: data.metrics,
            axisLine: {
                show: false
            },
            axisTick: {
                show: false
            }
        }, {
            type: 'category',
            data: data.coveredPercentageLabels,
            position: 'right',
            axisLine: {
                show: false
            },
            axisTick: {
                show: false
            }
        }],
        series: [
            {
                name: 'Covered',
                type: 'bar',
                stack: 'sum',
                itemStyle: {
                    normal: {
                        color: '#A5D6A7'
                    }
                },
                emphasis: {
                    focus: 'series'
                },
                data: data.coveredPercentage
            },
            {
                name: 'Missed',
                type: 'bar',
                stack: 'sum',
                itemStyle: {
                    normal: {
                        color: '#EF9A9A'
                    }
                },
                emphasis: {
                    focus: 'series'
                },
                data: data.missedPercentage
            }
        ]
    };

    option && portletChart.setOption(option)
}