getJenkinsColors = function (colors) {
    // TODO: also handle HSL colors and parse them to hex in order to use dark mode colors
    const colorHexMapping = new Map;
    colors.forEach(function (jenkinsId) {
        const colorHex = getComputedStyle(document.body).getPropertyValue(jenkinsId) || '#333';
        colorHexMapping.set(jenkinsId, colorHex);
    })
    return colorHexMapping;
};

