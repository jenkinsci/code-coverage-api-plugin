package io.jenkins.plugins.coverage.adapter.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import io.jenkins.plugins.coverage.exception.CoverageException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class JSONUtils {

    public static JSONObject readToJSONObject(File source) throws CoverageException {
        try (FileInputStream fis = new FileInputStream(source)) {
            return JSON.parseObject(fis, JSONObject.class);
        } catch (IOException e) {
            throw new CoverageException(e);
        }
    }

}
