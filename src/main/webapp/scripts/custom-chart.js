var CoverageChartGenerator = function () {

    function transformURL(name) {
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
    }

    /**
     * To display name like "&lt;init&gt;" or "&amp;lt;init&amp;gt;" as "<init>".
     */
    function transformXML(name) {
        return name.replace(/&amp;/g, "&").replace(/&lt;/g, "<").replace(/&gt;/g, ">");
    }

    this.generateSummaryChart = function (results, id, name, isTitleHasLink) {

        var summaryChartDiv = document.getElementById(id);
        if (!summaryChartDiv) {
            return;
        }

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

        summaryChartDiv.style.height = metrics.length * 31 + 200 + "px";
        var summaryChart = echarts.init(summaryChartDiv);

        const textColor = getComputedStyle(document.body).getPropertyValue('--text-color') || '#333';

        var stackedBarOption = {

            // Configuration like below won't lead to injection attack, because the text is displayed "as is".
            // So the transformation is all right, and it does help displaying name like "&lt;init&gt;" or "&amp;lt;init&amp;gt;" as "<init>".
            //
            // title: {
            //     text: "<script>alert(\"inject test\")</script>>"
            // },

            title: {
                text: transformXML(name) + (isTitleHasLink?' (click to see more details)':''),
                textStyle: {
                    color: textColor
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

            tooltip: {
                trigger: 'axis',
                axisPointer: {
                    type: 'shadow'
                },
                formatter: function (obj) {
                    if (Array.isArray(obj)) {
                        if (obj.length === 2) {
                            return '<b>' + obj[0].name + '</b><br/>' + obj[0].seriesName + ':' + covered[obj[0].dataIndex] + '<br/>' + obj[1].seriesName + ':' + missed[obj[1].dataIndex];
                        } else if (obj.length === 1) {
                            return '<b>' + obj[0].name + '</b><br/>' + obj[0].seriesName + ':' + (obj[0].seriesName === 'Covered' ? covered[obj[0].dataIndex] : missed[obj[0].dataIndex]);
                        }

                    }
                }
            },
            legend: {
                data: ['Covered', 'Missed'],
                right: 10,
                top: 25,
                textStyle: {
                    color: textColor
                }
            },
            grid: {
                left: '3%',
                right: '4%',
                bottom: '3%',
                containLabel: true
            },
            xAxis: {
                type: 'value',
                axisLabel: {
                    color: textColor
                }
            },
            yAxis: [{
                type: 'category',
                data: metrics,
                axisLine: {
                    show: false
                },
                axisTick: {
                    show: false
                },
                axisLabel: {
                    color: textColor
                }
            }, {
                type: 'category',
                data: coveredPercentage,
                position: 'right',
                axisLabel: {
                    formatter: function (value, index) {
                        return coveredPercentage[index].toFixed(2) + "%";
                    },
                    color: textColor
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
        if(isTitleHasLink) {
            stackedBarOption.title.link = transformURL(name);
            stackedBarOption.title.target = 'self';
        }

        summaryChart.setOption(stackedBarOption);


        window.onresize = function (ev) {
            summaryChart.resize();
        }

    };

    this.generateChildSummaryChart = function (results, id, metric) {

        var childSummaryChartDiv = document.getElementById(id);
        if (!childSummaryChartDiv) {
            return;
        }

        var defaultRange = [0, 100];


        var r = filterChildren(results, defaultRange);
        if (r.children.length === 0) {
            defaultRange = [0, 100];
            r = filterChildren(results, defaultRange);
        }
        var metrics = r.metrics;
        var children = r.children;
        var data = r.data;


        var height = children.length * 32 + 120;
        if (height < 180) {
            height = 180;
        }
        childSummaryChartDiv.style.height = height + "px";
        var childSummaryChart = echarts.init(childSummaryChartDiv);

        const textColor = getComputedStyle(document.body).getPropertyValue('--text-color') || '#333';

        var childSummaryChartOption = {
            title: {
                text: 'Divided by ' + (metric ? metric + ' Name' : ''),
                subtext: "click item name to see more details",
                textStyle: {
                    color: textColor
                }
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
                top: '60',
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
                    fontWeight: 'bold',
                    color: textColor
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
                    interval: 0,
                    fontSize: 13,
                    formatter: function (value) {
                        if(value.length > 70) {
                            return value.substring(0, 70)+'...';
                        }
                        return value;
                    },
                    color: textColor
                }
            },
            visualMap: {
                min: 0,
                max: 100,
                range: defaultRange,
                calculable: true,
                orient: 'horizontal',
                left: 'right',
                top: 'top',
                inRange: {
                    color: ['#d55e00', '#009e73']
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

        childSummaryChart.setOption(childSummaryChartOption);
        childSummaryChart.on('click', function (params) {
            if (params.componentType === 'yAxis') {
                window.location.href = transformURL(params.value);
            }
        });

        childSummaryChart.on('datarangeselected', function (params) {


            var r = filterChildren(results, params.selected);
            var children = r.children;
            var data = r.data;


            if (children.length !== 0) {
                childSummaryChartOption.series[0].data = data;
            } else {
                childSummaryChartOption.series[0].data = null;
            }

            childSummaryChartOption.visualMap.range = [params.selected[0], params.selected[1]];
            childSummaryChartOption.yAxis.data = children;

            var height = children.length * 32 + 120;
            if (height < 180) {
                height = 180;
            }
            childSummaryChartDiv.style.height = height + "px";

            var childSummaryChart = echarts.init(childSummaryChartDiv);

            childSummaryChart.setOption(childSummaryChartOption);
            childSummaryChart.resize();
        });

        window.onresize = function () {
            childSummaryChart.resize();
        }
    };

    this.generateTrendChart = function (results, id) {
        var trendChartDiv = document.getElementById(id);
        if (!trendChartDiv) {
            return;
        }

        trendChartDiv.style.height = '400px';
        var trendChart = echarts.init(trendChartDiv);


        var builds = [];
        var metrics = [];
        var series = [];

        Object.keys(results).reverse().forEach(function (buildName, buildIndexInResults) {
            builds.push(buildName);

            results[buildName].forEach(function (coverage, metricIndex) {
                var ratio = coverage.ratio;
                metrics[metricIndex] = coverage.name;

                if (!series[metricIndex]) {
                    series[metricIndex] = {
                        name: coverage.name,
                        type: 'line',
                        data: []
                    }
                }
                series[metricIndex].data.push(ratio.percentageFloat);

            });

        });

        const textColor = getComputedStyle(document.body).getPropertyValue('--text-color') || '#333';

        var option = {
            title: {
                text: 'Trend',
                textStyle: {
                    color: textColor
                }
            },
            tooltip: {
                trigger: 'item',
                formatter: function (obj) {
                    return '<b>' + obj.name + '</b><br />' + obj.seriesName + ': ' + obj.value + '%';
                }
            },
            legend: {
                data: metrics,
                textStyle: {
                    color: textColor
                }
            },
            toolbox: {
                feature: {
                    saveAsImage: {}
                },
                bottom: 0
            },
            grid: {
                containLabel: true
            },
            dataZoom: {
                type: 'inside',
                yAxisIndex: [0],
                orient: 'vertical',
                textStyle: {
                    color: textColor
                }
            },
            xAxis: {
                type: 'category',
                boundaryGap: false,
                data: builds,
                axisLabel: {
                    color: textColor
                }
            },
            yAxis: {
                type: 'value',
                axisLabel: {
                    color: textColor
                }
            },
            series: series
        };

        trendChart.setOption(option);

    };

    function filterChildren(results, range) {
        var metrics = [];
        var children = [];

        var data = [];

        Object.keys(results).sort().filter(function (childName, childIndex) {
            return results[childName].find(function (element) {
                var p = parseFloat(element.ratio.percentageString);

                if(element.ratio.denominator === 0) {
                    p = 0;
                }

                return p >= range[0] && p <= range[1];
            });
        }).forEach(function (childName, childIndex) {
            children.push(childName);
            results[childName].forEach(function (coverage, metricIndex) {
                var ratio = coverage.ratio;

                metrics[metricIndex] = coverage.name;

                if(ratio.denominator !== 0) {
                    data.push([metricIndex, childIndex, parseFloat(ratio.percentageString)]);
                }
            })
        });

        return {
            metrics: metrics,
            children: children,
            data: data
        }
    }
};
