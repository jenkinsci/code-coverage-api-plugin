/* global jQuery3, viewProxy, echartsJenkinsApi, bootstrap5 */
(function ($) {
    function printPercentage(value) {
        return Number(value).toLocaleString(undefined, {style: 'percent', minimumFractionDigits: 2});
    }

    const openBuild = function (build) {
        viewProxy.getUrlForBuild(build, window.location.href, function (buildUrl) {
            if (buildUrl.responseJSON.startsWith('http')) {
                window.location.assign(buildUrl.responseJSON);
            }
        });
    };

    function createOverview(overview, id) {
            const summaryChartDiv = document.getElementById(id);
            summaryChartDiv.style.height = overview.elements.length * 31 + 150 + "px";
            const summaryChart = echarts.init(summaryChartDiv);
            summaryChartDiv.echart = summaryChart;

            const textColor = getComputedStyle(document.body).getPropertyValue('--text-color') || '#333';

            const summaryOption = {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'shadow'
                    },
                    formatter: function (obj) {
                        if (Array.isArray(obj)) {
                            if (obj.length === 2) {
                                return ['<b>' + obj[0].name + '</b>',
                                    obj[0].seriesName + ': ' + overview.covered[obj[0].dataIndex],
                                    obj[1].seriesName + ': ' + overview.missed[obj[1].dataIndex],
                                    printPercentage(overview.coveredPercentages[obj[0].dataIndex])

                                ].join('<br/>');
                            }
                            else if (obj.length === 1) {
                                return '<b>' + obj[0].name + '</b><br/>'
                                    + obj[0].seriesName + ': ' + (obj[0].seriesName === 'Covered' ? overview.covered[obj[0].dataIndex] : overview.missed[obj[0].dataIndex]);
                            }

                        }
                    }
                },
                legend: {
                    data: ['Covered', 'Missed'],
                    x: 'center',
                    y: 'top',
                    textStyle: {
                        color: textColor
                    }
                },
                grid: {
                    left: '20',
                    right: '10',
                    bottom: '5',
                    top: '40',
                    containLabel: true
                },
                xAxis: {
                    type: 'value',
                    axisLabel: {
                        formatter: function (value) {
                            return printPercentage(value);
                        },
                        color: textColor
                    }
                },
                yAxis: [{
                    type: 'category',
                    data: overview.elements,
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
                    data: overview.coveredPercentages,
                    position: 'right',
                    axisLabel: {
                        formatter: function (value, index) {
                            return printPercentage(overview.coveredPercentages[index]);
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
                                color: coveredColor
                            }
                        },
                        label: {
                            show: true,
                            position: 'insideLeft',
                            formatter: function (obj) {
                                return overview.covered[obj.dataIndex];
                            }
                        },
                        data: overview.coveredPercentages
                    },
                    {
                        name: 'Missed',
                        type: 'bar',
                        stack: 'sum',
                        itemStyle: {
                            normal: {
                                color: missedColor
                            }
                        },
                        label: {
                            show: true,
                            position: 'insideRight',
                            formatter: function (obj) {
                                return overview.missed[obj.dataIndex];
                            }
                        },
                        data: overview.missedPercentages
                    }
                ]
            };
            summaryChart.setOption(summaryOption);

            window.addEventListener('resize', function () {
                summaryChart.resize();
            });
        }

    function createFilesTreeMap (coverageTree, id) {
            function getLevelOption () {
                return [
                    {
                        itemStyle: {
                            borderColor: 'black',
                            borderWidth: 0,
                            gapWidth: 1
                        },
                        upperLabel: {
                            show: false
                        }
                    },
                    {
                        itemStyle: {
                            borderColor: '#ddd',
                            borderWidth: 2,
                            gapWidth: 2
                        }
                    },
                    {
                        itemStyle: {
                            borderWidth: 4,
                            gapWidth: 2,
                            borderColorSaturation: 0.6
                        }
                    },
                    {
                        itemStyle: {
                            borderWidth: 4,
                            gapWidth: 2,
                            borderColorSaturation: 0.7
                        }
                    },
                    {
                        itemStyle: {
                            borderWidth: 4,
                            gapWidth: 2,
                            borderColorSaturation: 0.6
                        }
                    },
                    {
                        itemStyle: {
                            borderWidth: 4,
                            gapWidth: 2,
                            borderColorSaturation: 0.7
                        }
                    },
                    {
                        itemStyle: {
                            borderWidth: 4,
                            gapWidth: 2,
                            borderColorSaturation: 0.6
                        }
                    },
                    {
                        itemStyle: {
                            borderWidth: 4,
                            gapWidth: 2,
                            borderColorSaturation: 0.7
                        }
                    },
                    {
                        itemStyle: {
                            borderWidth: 4,
                            gapWidth: 2,
                            borderColorSaturation: 0.6
                        }
                    },
                    {
                        itemStyle: {
                            borderWidth: 4,
                            gapWidth: 2,
                            borderColorSaturation: 0.7
                        }
                    },
                ];
            }

            const treeChartDiv = document.getElementById(id);
            const treeChart = echarts.init(treeChartDiv);
            treeChartDiv.echart = treeChart;

            const textColor = getComputedStyle(document.body).getPropertyValue('--text-color') || '#333';
            const formatUtil = echarts.format;

            const option = {
                tooltip: {
                    formatter: function (info) {
                        const treePathInfo = info.treePathInfo;
                        const treePath = [];
                        for (let i = 2; i < treePathInfo.length; i++) {
                            treePath.push(treePathInfo[i].name);
                        }

                        const values = info.value;
                        const total = values[0];
                        const covered = values[1];

                        const title = '<div class="chart-tooltip-title">' + formatUtil.encodeHTML(treePath.join('.')) + '</div>';
                        if (total === 0) {
                            return [title, 'Line Coverage: n/a',].join('');
                        }
                        return [
                            title,
                            'Line Coverage: ' + printPercentage(covered / total),
                            ' (' + 'covered: ' + covered + ', missed: ' + (total - covered) + ')',
                        ].join('');
                    }
                },
                series: [
                    {
                        name: 'Line Coverage',
                        type: 'treemap',
                        breadcrumb: {
                            itemStyle: {
                                color: '#A4A4A4'
                            },
                            emphasis: {
                                itemStyle: {
                                    opacity: 0.6
                                },
                            }
                        },
                        width: '100%',
                        height: '95%',
                        top: 'top',
                        label: {
                            show: true,
                            formatter: '{b}',
                            color: textColor
                        },
                        upperLabel: {
                            show: true,
                            height: 30,
                            color: 'black',
                            borderColorSaturation: 0.6,
                            colorSaturation: 0.6,
                        },
                        itemStyle: {
                            borderColor: '#fff',
                        },
                        levels: getLevelOption(),
                        data: [coverageTree]
                    }
                ]
            };
            treeChart.setOption(option);
            window.addEventListener('resize', function () {
                treeChart.resize();
            });
        }

    function redrawTrendChart() {
        const configuration = JSON.stringify(echartsJenkinsApi.readFromLocalStorage('jenkins-echarts-chart-configuration-coverage-history'));

        viewProxy.getTrendChart(configuration, function (t) {
            echartsJenkinsApi.renderConfigurableZoomableTrendChart('coverage-trend', t.responseJSON, 'chart-configuration-coverage-history', openBuild);
        });
    }

    const missedColor = '#EF9A9A';
    const coveredColor = '#A5D6A7';

    const trendConfigurationDialogId = 'chart-configuration-coverage-history';

    $('#' + trendConfigurationDialogId).on('hidden.bs.modal', function () {
        redrawTrendChart();
    });

    viewProxy.getOverview(function (t) {
        createOverview(t.responseObject(), 'coverage-overview');

        $('#coverage-trend').height($('#coverage-overview').height());

        redrawTrendChart();
    });

    viewProxy.getCoverageTree(function (t) {
        createFilesTreeMap(t.responseObject(), 'coverage-details');
    });

})(jQuery3);
