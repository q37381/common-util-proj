package com.common.util;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

/**
 * word表格转换工具
 * 
 * @author Nbb
 *
 */
public class WordTableTranslateUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(WordTableTranslateUtils.class);

    /**
     * 将表格文字按制表符组装成list，均转换为小写
     * 
     * @param doc
     * @return
     */
    public static List<List<String>> splitWordToListByTab(String doc) {

        String[] splitDocArr = splitDocToLineArr(doc.toLowerCase());

        return transToListString(splitDocArr);

    }

    private static List<List<String>> transToListString(String[] splitDocArr) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("第一次切分，切分出每一行：");
            for (int i = 0; i < splitDocArr.length; i++) {
                LOGGER.debug("{}={}", i, splitDocArr[i]);
            }
            LOGGER.debug("第一次切分结束========");
        }

        List<List<String>> list = Lists.newArrayList();
        for (String first : splitDocArr) {
            // 空行跳过
            if (StringUtils.isBlank(first)) {
                continue;
            }

            // 第二次切分一行中的列
            String[] splitColumn = first.split("\\t");
            List<String> columnList = Lists.newArrayList();

            LOGGER.debug("第二次切分，切分出每行的列：");
            for (int j = 0; j < splitColumn.length; j++) {
                String column = splitColumn[j];
                columnList.add(column.trim());
                LOGGER.debug("{}={}", j, column);
            }
            list.add(columnList);

            LOGGER.debug("第二次切分结束==========");
        }

        return list;
    }

    private static String[] splitDocToLineArr(String doc) {
        String[] split = doc.split("\\r\\n");
        if (split == null || split.length == 1) {
            split = doc.split("[\\r\\n]");
        }

        return split;
    }

}
