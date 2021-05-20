
var CoveragePortletChart = function (results, id) {

    var metrics = [];
    var covered = [];
    var missed = [];
    var coveredPercentage = [];
    var missedPercentage = [];
    for (var i = 0; i < results.length; i++) {
        metrics[i] = results[i].name;
        covered[i] = results[i].ratio.numerator;
        missed[i] = results[i].ratio.denominator - covered[i];

        coveredPercentage[i] = 100 * (covered[i] / results[i].ratio.denominator);
        missedPercentage[i] = 100 - coveredPercentage[i];

        if (results[i].ratio.denominator === 0) {
            coveredPercentage[i] = 0;
        }

    }

    const chartDom = document.getElementById(id);
    const portletChart = echarts.init(chartDom);

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
            name: 'Metric',
            type: 'category',
            data: metrics,
            axisLine: {
                show: false
            },
            axisTick: {
                show: false
            }
        }, {
            type: 'category',
            data: coveredPercentage,
            position: 'right',
            axisLabel: {
                formatter: function (value, index) {
                    return coveredPercentage[index].toFixed(2) + "%";
                }
            },
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
                data: coveredPercentage
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
                data: missedPercentage
            }
        ]
    };

    option && portletChart.setOption(option)
}