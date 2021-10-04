/* global jQuery3, viewProxy, echartsJenkinsApi, bootstrap5 */
const CoverageChartGenerator = function ($) {
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

    function getTextColor() {
        return getComputedStyle(document.body).getPropertyValue('--text-color') || '#333';
    }

    function createOverview(overview, id) {
        const missedColor = '#EF9A9A';
        const coveredColor = '#A5D6A7';

        const summaryChartDiv = $('#' + id);
        summaryChartDiv.height(overview.metrics.length * 31 + 150 + 'px');
        const summaryChart = echarts.init(summaryChartDiv[0]);
        summaryChartDiv[0].echart = summaryChart;

        const textColor = getTextColor();

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
                                + obj[0].seriesName + ': '
                                + (obj[0].seriesName === 'Covered' ?
                                    overview.covered[obj[0].dataIndex]
                                    : overview.missed[obj[0].dataIndex]);
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
                data: overview.metrics,
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

    function createFilesTreeMap(coverageTree, id) {
        function getLevelOption() {
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

        const treeChartDiv = $('#' + id);
        const treeChart = echarts.init(treeChartDiv[0]);
        treeChartDiv[0].echart = treeChart;

        const textColor = getTextColor();
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

    this.populateDetailsCharts = function () {
        const trendConfigurationDialogId = 'chart-configuration-coverage-history';

        $('#' + trendConfigurationDialogId).on('hidden.bs.modal', function () {
            redrawTrendChart();
        });

        viewProxy.getOverview(function (t) {
            createOverview(t.responseObject(), 'coverage-overview');

            $('#coverage-trend').height($('#coverage-overview').height());

            redrawTrendChart();
        });

        /**
         * Activate the tab that has been visited the last time. If there is no such tab, highlight the first one.
         * If the user selects the tab using an #anchor prefer this tab.
         */
        selectTab('li:first-child a');
        const url = document.location.toString();
        if (url.match('#')) {
            const tabName = url.split('#')[1];
            selectTab('a[href="#' + tabName + '"]');
        }
        else {
            const activeTab = localStorage.getItem('activeTab');
            if (activeTab) {
                selectTab('a[href="' + activeTab + '"]');
            }
        }

        /**
         * Store the selected tab in browser's local storage.
         */
        const tabToggleLink = $('a[data-bs-toggle="tab"]');
        tabToggleLink.on('show.bs.tab', function (e) {
            window.location.hash = e.target.hash;
            const activeTab = $(e.target).attr('href');
            localStorage.setItem('activeTab', activeTab);
        });

        viewProxy.getCoverageTree(function (t) {
            createFilesTreeMap(t.responseObject(), 'coverage-details');
        });
    }

    /**
     * Activates the specified tab.
     *
     * @param {String} selector - selector of the tab
     */
    function selectTab (selector) {
        const detailsTabs = $('#tab-details');
        const selectedTab = detailsTabs.find(selector);

        if (selectedTab.length !== 0) {
            const tab = new bootstrap5.Tab(selectedTab[0]);
            tab.show();
        }
    }


    this.populateOverview = function () {
        viewProxy.getOverview(function (t) {
            createOverview(t.responseObject(), 'coverage-overview');
        });
    }
};
