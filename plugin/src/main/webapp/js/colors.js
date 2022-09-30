getJenkinsColors = function (colors) {
    const colorHexMapping = new Map;
    colors.forEach(function (jenkinsId) {
        const colorHex = getComputedStyle(document.body).getPropertyValue(jenkinsId) || '#333';
        colorHexMapping.set(jenkinsId, colorHex);
    })
    return colorHexMapping;
};

