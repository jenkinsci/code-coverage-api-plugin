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
        window.addEventListener('resize', function () {
            treeChart.resize();
        });
        $('a[data-bs-toggle="tab"]').on('shown.bs.tab', function () {
            treeChart.resize();
        });

        treeChart.resize();
    }

    this.populateDetailsCharts = function () {
        initializeReportView();

        viewProxy.getOverview(function (t) {
            createOverview(t.responseObject(), 'coverage-overview');

            $('#coverage-trend').height($('#coverage-overview').height());
        });

        viewProxy.getCoverageTree(function (t) {
            createFilesTreeMap(t.responseObject(), 'project-coverage');
        });

        // only required when the change-coverage div is visible
        if (document.getElementById('change-coverage')) {
            viewProxy.getChangeCoverageTree(function (t) {
                createFilesTreeMap(t.responseObject(), 'change-coverage');
            });
            viewProxy.getChangeCoverageOverview(function (t) {
                createOverview(t.responseObject(), 'change-coverage-overview');
                $('#change-coverage-overview').height($('#coverage-overview').height());
            });
        }

        // only required when the coverage-changes div is visible
        if (document.getElementById('coverage-changes')) {
            viewProxy.getCoverageChangesTree(function (t) {
                createFilesTreeMap(t.responseObject(), 'coverage-changes');
            });
        }

        viewProxy.hasStoredSourceCode(function (t) {
            if (t.responseObject()) {
                initializeDataTables();
            }
        });
    }


///////////////////////////////////////////////////////////////////////
// Report view initialization
///////////////////////////////////////////////////////////////////////

    function initializeReportView() {
        $('#report-overview a[data-bs-toggle="tab"]').on('show.bs.tab', function (e) {
            window.location.hash = e.target.hash;
            const activeTab = $(e.target).attr('href');
            localStorage.setItem('activeTabOverview', activeTab);
        });

        $('#report-details a[data-bs-toggle="tab"]').on('show.bs.tab', function (e) {
            window.location.hash = e.target.hash;
            const activeTab = $(e.target).attr('href');
            localStorage.setItem('activeTabDetails', activeTab);
        });

        const url = document.location.toString();
        // prefer loading the report by a set anchor
        if (url.match('#')) {
            const tabName = url.split('#')[1];
            const loaded = loadView('#' + tabName);
            if (!loaded) {
                // the default
                loadView('#coverageTree');
            }
        }
        else {
            let activeTab;
            let defaultTab;
            const view = localStorage.getItem("activeReport");
            if (view === "report-overview") {
                activeTab = localStorage.getItem('activeTabOverview');
                defaultTab = '#coverageTree';
            }
            else if (view === "report-details") {
                activeTab = localStorage.getItem('activeTabDetails');
                defaultTab = '#coverageTable';
            }
            else {
                if (document.getElementById("report-overview").classList.contains("d-none")) {
                    activeTab = localStorage.getItem('activeTabDetails');
                    defaultTab = '#coverageTable';
                }
                else {
                    activeTab = localStorage.getItem('activeTabOverview');
                    defaultTab = '#coverageTree';
                }
            }
            if (!loadView(activeTab)) {
                loadView(defaultTab);
            }
        }

        initializeOverviewColumnVisibility();
    }

    /**
     * Initializes the visibility of the #overview-column.
     */
    function initializeOverviewColumnVisibility() {
        const showCharts = localStorage.getItem('showOverviewColumn');
        if (showCharts) {
            const show = showCharts === 'true';
            setOverviewColumnVisibility(show);
            $('#showOverviewChartsToggle').prop('checked', show);
        }
        else {
            setOverviewColumnVisibility(true);
            $('#showOverviewChartsToggle').prop('checked', true);
        }
    }

    /**
     * Initializes the datatables.
     */
    function initializeDataTables() {
        $(document).ready(function () {
            initializeSourceCodeSelection($('#coverage-table').DataTable(), 'coverage-table');
            initializeSourceCodeSelection($('#change-coverage-table').DataTable(), 'change-coverage-table');
            initializeSourceCodeSelection($('#coverage-changes-table').DataTable(), 'coverage-changes-table');
        });
    }

    /**
     * Initializes a selection listener for a datatable which loads the selected source code.
     *
     * @param {DataTable} datatable The DataTable
     * @param {String} id The ID of the DataTable
     */
    function initializeSourceCodeSelection(datatable, id) {
        datatable.on('select', function (e, dt, type, indexes) {
            if (type === 'row') {
                $('#source-file').html('Loading...');
                const rowData = datatable.rows(indexes).data().toArray();
                // TODO: pass id to load specifically prepared source code
                viewProxy.getSourceCode(rowData[0].fileHash, function (t) {
                    const sourceCode = t.responseObject();
                    if (sourceCode === "n/a") {
                        $('#source-file').html("");
                        document.getElementById('source-file-unselected').classList.add("d-none");
                        document.getElementById('source-file-content').classList.add("d-none");
                        document.getElementById('source-file-unavailable').classList.remove("d-none");
                    }
                    else {
                        $('#source-file').html(sourceCode);
                        document.getElementById('source-file-unselected').classList.add("d-none");
                        document.getElementById('source-file-unavailable').classList.add("d-none");
                        document.getElementById('source-file-content').classList.remove("d-none");
                    }
                });
            }
            else {
                clearSourceCode();
            }
        })
        datatable.on('deselect', function () {
            clearSourceCode();
        });
    }

///////////////////////////////////////////////////////////////////////
// Report view management
///////////////////////////////////////////////////////////////////////

    /**
     * Loads a report view by its ID, selected by the user.
     *
     * @param {String} id The report view ID
     */
    this.loadReportViewById = function (id) {
        let loadedSuccessfully = false;
        if (id === 'report-overview') {
            const active = localStorage.getItem('activeTabOverview');
            if (active) {
                loadedSuccessfully = loadView(active);
            }
            if (!loadedSuccessfully) {
                loadView('#coverageTree');
            }
        }
        else if (id === 'report-details') {
            const active = localStorage.getItem('activeTabDetails');
            if (active) {
                loadedSuccessfully = loadView(active);
            }
            if (!loadedSuccessfully) {
                loadView('#coverageTable');
            }
        }
    }

    /**
     * Loads the report view with the passed id.
     *
     * @param {String} id The id of the report view
     */
    function loadView(id) {
        const selector = 'a[href="' + id + '"]';
        let detailsTabs = $('#tab-tree');
        let selectedTab = detailsTabs.find(selector);
        if (selectedTab.length !== 0) {
            showReportView("report-overview");
            selectTab(selectedTab);
            location.href = id;
            return true;
        }
        detailsTabs = $('#tab-details');
        selectedTab = detailsTabs.find(selector);
        if (selectedTab.length !== 0) {
            showReportView("report-details");
            selectTab(selectedTab);
            location.href = id;
            return true;
        }
        return false;
    }

    /**
     * Selects the passed Bootstrap tab.
     *
     * @param selectedTab The selected tab
     */
    function selectTab(selectedTab) {
        const tab = new bootstrap5.Tab(selectedTab[0]);
        tab.show(function () {
            fireResizeEvent();
        });
    }

    /**
     * Makes the report view with the passed id visible.
     *
     * @param {String} id The ID of the report div to be shown
     */
    function showReportView(id) {
        const overview = document.getElementById("report-overview");
        const details = document.getElementById("report-details");
        const hideOverviewButton = document.getElementById("hide-button");
        const menuSeparator = document.getElementById("menu-separator");

        if (id === overview.id) {
            details.classList.add("d-none");
            overview.classList.remove("d-none");
            menuSeparator.style.display = 'block';
            hideOverviewButton.style.display = 'block';
            $(document).ready(function () {
                $('#overviewToggle').prop('checked', true);
                $('#detailToggle').prop('checked', false);
                localStorage.setItem("activeReport", overview.id);
            });
        }
        else if (id === details.id) {
            menuSeparator.style.display = 'none';
            hideOverviewButton.style.display = 'none';
            overview.classList.add("d-none");
            details.classList.remove("d-none");
            $(document).ready(function () {
                $('#detailToggle').prop('checked', true);
                $('#overviewToggle').prop('checked', false);
                localStorage.setItem("activeReport", details.id);
            });
        }

        fireResizeEvent();
    }

    /**
     * Toggles the visibility of the #overview-column, triggered by the user.
     */
    this.toggleOverviewColumn = function () {
        const overview = document.getElementById("overview-column");
        setOverviewColumnVisibility(overview.style.display === "none");
    }

    /**
     * Sets the visibility of the #overview-column which contains the overview charts.
     *
     * @param {boolean} show true whether the column should be shown
     */
    function setOverviewColumnVisibility(show) {
        const div = document.getElementById("overview-column");
        const divTree = document.getElementById("tree-column");
        if (show) {
            div.style.display = "block";
            divTree.classList.add("ms-3");
        }
        else {
            div.style.display = "none";
            divTree.classList.remove("ms-3");
        }
        localStorage.setItem('showOverviewColumn', '' + show);
        fireResizeEvent();
        $(document).ready(function () {
            $('#showOverviewChartsToggle').prop('checked', show);
        });
    }

    /**
     * Clears the source code view within #source-file.
     */
    function clearSourceCode() {
        $('#source-file').html('');
        document.getElementById('source-file-content').classList.add("d-none");
        document.getElementById('source-file-unselected').classList.remove("d-none");
    }

    /**
     * Manually fires a resize event which is useful for adjusting canvas sizes with resize listeners.
     */
    function fireResizeEvent() {
        if (typeof (Event) === 'function') {
            // for modern browsers
            window.dispatchEvent(new Event('resize'));
        }
        else {
            // for IE and other old browsers
            const event = window.document.createEvent('UIEvents');
            event.initUIEvent('resize', true, false, window, 0);
            window.dispatchEvent(event);
        }
    }
};
