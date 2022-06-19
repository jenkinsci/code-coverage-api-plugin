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
        summaryChart.resize();
     }

    function createFilesTreeMap(coverageTree, id, coverageMetric) {
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
                        return [title, coverageMetric + ' Coverage: n/a',].join('');
                    }
                    return [
                        title,
                        coverageMetric + ' Coverage: ' + printPercentage(covered / total),
                        ' (' + 'covered: ' + covered + ', missed: ' + (total - covered) + ')',
                    ].join('');
                }
            },
            series: [
                {
                    name: coverageMetric + ' Coverage',
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
                    height: '100%',
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
        treeChart.resize();
    }

    this.populateDetailsCharts = function () {
        /**
         * Activate the tab that has been visited the last time. If there is no such tab, highlight the first one.
         * If the user selects the tab using an #anchor prefer this tab.
         */
        function registerTabEvents() {
            /**
             * Activates the specified tab.
             *
             * @param {String} selector - selector of the tab
             * @return true if the tab has been selected
             */
            function selectTab (selector) {
                const detailsTabs = $('#tab-details');
                const selectedTab = detailsTabs.find(selector);

                if (selectedTab.length !== 0) {
                    const tab = new bootstrap5.Tab(selectedTab[0]);
                    tab.show();

                    return true;
                }
                return false
            }

            const selectedTabID = 'jenkins-coverage-activeTab';
            const url = document.location.toString();
            if (url.match('#')) {
                const tabName = url.split('#')[1];
                if (selectTab('a[href="#' + tabName + '"]')) {
                    localStorage.setItem(selectedTabID, '#' + tabName);
                }

            }
            else {
                const activeTab = localStorage.getItem(selectedTabID);
                if (activeTab) {
                    selectTab('a[href="' + activeTab + '"]');
                }
            }
            if ($('#tab-details a.active').length === 0) {
                selectTab('li:first-child a'); // fallback if all other options fail
            }

            $('a[data-bs-toggle="tab"]').on('shown.bs.tab', function (e) {
                window.location.hash = e.target.hash;
                const activeTab = $(e.target).attr('href');
                localStorage.setItem(selectedTabID, activeTab);
                redrawCharts();
            });
        }

        /**
         * Loads all chart JSON models via AJAX calls from the server and renders the corresponding echarts.
         */
        function initializeCharts() {
            viewProxy.getOverview(function (t) {
                createOverview(t.responseObject(), 'coverage-overview');
            });

            viewProxy.getCoverageTree('Line', function (t) {
                createFilesTreeMap(t.responseObject(), 'project-line-coverage', 'Line');
            });

            viewProxy.getCoverageTree('Branch', function (t) {
                createFilesTreeMap(t.responseObject(), 'project-branch-coverage', 'Branch');
            });
        }

        /**
         * Event handler to resizes all charts.
         */
        function redrawCharts() {
            function resizeChartOf(selector) {
                $(selector)[0].echart.resize();
            }

            const configuration = JSON.stringify(echartsJenkinsApi.readFromLocalStorage('jenkins-echarts-chart-configuration-coverage-history'));
            viewProxy.getTrendChart(configuration, function (t) {
                echartsJenkinsApi.renderConfigurableZoomableTrendChart('coverage-trend', t.responseJSON, 'chart-configuration-coverage-history', openBuild);
                resizeChartOf('#coverage-trend');
            });

            resizeChartOf('#coverage-overview');
            resizeChartOf('#project-line-coverage');
            resizeChartOf('#project-branch-coverage');
        }

        function registerTrendChartConfiguration() {
            const trendConfigurationDialogId = 'chart-configuration-coverage-history';
            $('#' + trendConfigurationDialogId).on('hidden.bs.modal', function () {
                redrawCharts();
            });
        }

        /**
         * Initializes a selection listener for a datatable which loads the selected source code.
         *
         * @param {String} tableId The ID of the DataTable
         */
        function initializeSourceCodeSelection(tableId) {
            const datatable = $('#' + tableId).DataTable();
            const sourceView = $('#' + tableId + '-source-file');
            datatable.on('select', function (e, dt, type, indexes) {
                if (type === 'row') {
                    sourceView.html('Loading...');
                    const rowData = datatable.rows(indexes).data().toArray();
                    viewProxy.getSourceCode(rowData[0].fileHash, tableId, function (t) {
                        const sourceCode = t.responseObject();
                        if (sourceCode === "n/a") {
                            sourceView.html('No source code available');
                        }
                        else {
                            sourceView.html(sourceCode);
                        }
                    });
                }
                else {
                    sourceView.html('No file selected');
                }
            })
            datatable.on('deselect', function () {
                sourceView.html('No file selected');
            });
        }

        registerTrendChartConfiguration();
        registerTabEvents();

        initializeCharts();

        window.addEventListener('resize', function () {
            redrawCharts();
        });

        $(document).ready(function () {
            initializeSourceCodeSelection('absolute-coverage-table-inline');
            initializeSourceCodeSelection('change-coverage-table-inline');
            initializeSourceCodeSelection('indirect-coverage-table-inline');
        });
    }
};
