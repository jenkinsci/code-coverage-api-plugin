var generateStackedCoverageChart = function (instance, name) {

    instance.getResults(function (t) {
        var results = t.responseObject();

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
                coveredPercentage[i] = 100;
            }

        }

        var summaryChartDiv = document.getElementById('summaryChart');
        summaryChartDiv.style.height = metrics.length * 31 + 200 + "px";
        var summaryChart = echarts.init(summaryChartDiv);


        var stackedBarOption = {

            title: {
                text: name,
                x: 'center'
            },

            toolbox: {
                show: true,
                feature: {
                    saveAsImage: {
                        title: 'Save as image'
                    }
                },
                bottom: 0
            },

            tooltip: {
                trigger: 'axis',
                axisPointer: {
                    type: 'shadow'
                },
                formatter: function (obj) {
                    if (Array.isArray(obj)) {
                        return "<b>" + obj[0].name + "</b><br/>" + obj[0].seriesName + ":" + covered[obj[0].dataIndex] + "<br/>" + obj[1].seriesName + ":" + missed[obj[1].dataIndex];
                    }
                }
            },
            legend: {
                data: ['Covered', 'Missed'],
                right: 10
            },
            grid: {
                left: '3%',
                right: '4%',
                bottom: '3%',
                containLabel: true
            },
            xAxis: {
                type: 'value'

            },
            yAxis: [{
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
                            color: '#009e73'
                        }
                    },
                    label: {
                        show: true,
                        position: 'insideLeft',
                        formatter: function (obj) {
                            return covered[obj.dataIndex];
                        }
                    },
                    data: coveredPercentage
                },
                {
                    name: 'Missed',
                    type: 'bar',
                    stack: 'sum',
                    itemStyle: {
                        normal: {
                            color: '#d55e00'
                        }
                    },
                    label: {
                        show: true,
                        position: 'insideRight',
                        formatter: function (obj) {
                            return missed[obj.dataIndex];
                        }
                    },
                    data: missedPercentage
                }
            ]
        };
        summaryChart.setOption(stackedBarOption);

        window.onresize = function (ev) {
            summaryChart.resize();
        }

    });
};

var generateHeatMapCoverageChart = function (instance, metric) {


    instance.getChildResults(function (t) {
        var results = t.responseObject();

        var metrics = [];
        var children = [];

        var data = [];

        Object.keys(results).forEach(function (childName, childIndex) {
            children.push(childName);
            results[childName].forEach(function (coverage, metricIndex) {
                var ratio = coverage.ratio;

                metrics[metricIndex] = coverage.name;

                data.push([metricIndex, childIndex, ratio.percentage]);
            })
        });

        var childSummaryChartDiv = document.getElementById('childSummaryChart');
        var height = children.length * 32 + 100;
        if (height < 150) {
            height = 150;
        }
        childSummaryChartDiv.style.height = height + "px";
        var childSummaryChart = echarts.init(childSummaryChartDiv);


        var option = {
            title: {
                text: 'Divided by ' + (metric ? metric + ' Name' : '')
            },

            tooltip: {
                position: 'top',
                formatter: function (obj) {
                    var childName = children[obj.value[1]];
                    var ratio = results[childName][obj.value[0]].ratio;
                    var coveredString = ratio.numerator + "/" + ratio.denominator;
                    return '<b>' + childName + '</b>:' + obj.name + '<br/>' + obj.value[2] + '% (' + coveredString + ')<br/> ';
                }
            },
            toolbox: {
                show: true,
                feature: {
                    saveAsImage: {
                        title: 'Save as image'
                    }
                },
                bottom: 0
            },
            animation: false,
            grid: {
                top: '40',
                left: '0%',
                y: '10%',
                containLabel: true
            },
            xAxis: {
                type: 'category',
                data: metrics,
                position: 'top',
                splitArea: {
                    show: true
                },
                axisLine: {
                    show: false
                },
                axisTick: {
                    show: false
                },
                axisLabel: {
                    interval: 0,
                    fontWeight: 'bold'
                }
            },
            yAxis: {
                type: 'category',
                data: children,
                inverse: true,
                position: 'right',
                splitArea: {
                    show: true
                },
                axisLine: {
                    show: false
                },
                axisTick: {
                    show: false
                },
                triggerEvent: true,
                axisLabel: {
                    fontSize: 13
                }
            },
            visualMap: {
                min: 0,
                max: 100,
                calculable: true,
                orient: 'horizontal',
                left: 'center',
                inRange: {
                    color: ['#d55e00', '#009e73'],
                }
            },
            series: [{
                name: 'Coverage',
                type: 'heatmap',
                data: data,
                label: {
                    normal: {
                        show: true,
                        formatter: function (obj) {
                            return obj.data[2] + '%';
                        }
                    }
                },
                itemStyle: {
                    emphasis: {
                        shadowBlur: 10,
                        shadowColor: 'rgba(0, 0, 0, 0.5)'
                    }
                }
            }]
        };

        childSummaryChart.setOption(option);
        childSummaryChart.on('click', function (params) {
            if (params.componentType === 'yAxis') {
                window.location.href = transformURL(params.value);
            }
        });

        window.onresize = function (ev) {
            childSummaryChart.resize();
        }
    });


};

var transformURL = function (name) {
    var s = '';
    for (var i = 0; i < name.length; i++) {
        var c = name.charAt(i);
        if (('0' <= c && '9' >= c)
            || ('A' <= c && 'Z' >= c)
            || ('a' <= c && 'z' >= c)) {
            s += c;
        } else {
            s += '_';
        }
    }
    return s;
};