package com.saic.uicds.core.infrastructure.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WorkProductSqlBuilder {

    private Map<String, String> tableParams = new HashMap<String, String>();

    private Map<String, String> tableOps = new HashMap<String, String>();

    private int startIndex;

    private int count;

    private String TABLE_NAME = "workproducts";

    public WorkProductSqlBuilder() {

        tableParams.put("productid", "ProductID");
        tableParams.put("producttypeversion", "ProductTypeVersion");
        tableParams.put("productversion", "ProductVersion");
        tableParams.put("producttype", "WPType");
        tableParams.put("productstate", "State");
        tableParams.put("createdbegin", "Created");
        tableParams.put("createdend", "Created");
        tableParams.put("createdby", "CreatedBy");
        tableParams.put("updatedbegin", "LastUpdated");
        tableParams.put("updatedend", "LastUpdated");
        tableParams.put("updatedby", "LastUpdatedBy");
        tableParams.put("mimetype", "MimeType");
        tableParams.put("interestgroup", "AssociatedGroups");
        tableOps.put("productid", "=");
        tableOps.put("producttypeversion", "like");
        tableOps.put("productversion", "=");
        tableOps.put("producttype", "like");
        tableOps.put("productstate", "like");
        tableOps.put("createdbegin", ">=");
        tableOps.put("createdend", "<=");
        tableOps.put("createdby", "like");
        tableOps.put("updatedbegin", ">=");
        tableOps.put("updatedend", "<=");
        tableOps.put("updatedby", "like");
        tableOps.put("mimetype", "like");
        tableOps.put("interestgroup", "like");
    }

    private String buildInnerQuery(boolean hasCount, Map<String, String[]> queryParams) {

        String top = "";
        if (hasCount) {
            int topCount = getStartIndex() + getCount();
            top = "TOP " + topCount + " ";
        }
        String paramQuery = buildParamQuery(queryParams);
        // String query = "(SELECT " + top
        // + "ROW_NUMBER() OVER (ORDER BY ID ASC) AS Row, ID,RawXML FROM "
        // + TABLE_NAME + paramQuery+")";
        String query = "(SELECT " + top + "ROW_NUMBER() OVER (ORDER BY ID ASC) AS Row, * FROM "
            + TABLE_NAME + paramQuery + ")";
        return query;
    }

    private String buildParamQuery(Map<String, String[]> queryParams) {

        if (queryParams.isEmpty())
            return "";
        StringBuffer query = new StringBuffer();
        query.append(" WHERE ");
        int curParam = 0;
        for (String key : queryParams.keySet()) {
            query.append("(");
            query.append(buildSqlParamStatement(key.toLowerCase(), queryParams.get(key)));
            query.append(") ");
            if (queryParams.size() > 1 && curParam < queryParams.size() - 1) {
                query.append(" AND ");
            }
            curParam++;
        }
        return query.toString();
    }

    public String buildQuery(Map<String, String[]> params) {

        if (params.containsKey("startIndex")) {
            String startIndex = params.get("startIndex")[0];
            setStartIndex(Integer.parseInt(startIndex));
        } else {
            setStartIndex(1);
        }
        boolean hasCount = false;
        if (params.containsKey("count")) {
            String count = params.get("count")[0];
            setCount(Integer.parseInt(count));
            hasCount = true;
        }
        /*
         * DECLARE @startIndex int SET @startIndex = 26 DECLARE @count int SET
         * @count = 10 SELECT * FROM (SELECT TOP (@startIndex+@count)
         * ROW_NUMBER() OVER (ORDER BY ID ASC) AS Row, ID,RawXML FROM
         * workproducts) AS WPwithRowNos WHERE Row Between @startIndex AND
         * (@startIndex+@count-1)
         */
        Map<String, String[]> queryParams = refineParams(params);
        StringBuilder query = new StringBuilder();
        query.append(buildSqlBaseStatement());
        String innerQuery = buildInnerQuery(hasCount, queryParams);
        query.append(innerQuery);
        query.append(" AS WPwithRowNos WHERE Row >=" + getStartIndex());
        if (hasCount) {
            int count = getStartIndex() + getCount() - 1;
            query.append(" AND Row<=" + count);
        }
        return query.toString();
    }

    private String buildSqlBaseStatement() {

        return "SELECT * FROM ";
    }

    private Object buildSqlParamStatement(String key, String[] values) {

        StringBuilder paramQuery = new StringBuilder();
        String column = tableParams.get(key);
        String op = tableOps.get(key);
        int current = 0;
        int valuesLength = values.length;
        for (String value : values) {
            paramQuery.append(column);
            paramQuery.append(" " + op + " ");
            if (!op.equals("="))
                value = "'" + value + "'";
            if (op.equals("=") && key.equals("productid"))
                value = "'" + value + "'";
            paramQuery.append(value);
            if (valuesLength > 1 && current < valuesLength - 1)
                paramQuery.append(" OR ");
            current++;
        }
        return paramQuery;
    }

    public int getCount() {

        return count;
    }

    public int getStartIndex() {

        return startIndex;
    }

    private Map<String, String[]> refineParams(Map<String, String[]> params) {

        Set<String> tableKeys = tableParams.keySet();
        Map<String, String[]> result = new HashMap<String, String[]>();
        for (String key : params.keySet()) {
            if (tableKeys.contains(key.toLowerCase())) {
                result.put(key, params.get(key));
            }
        }
        return result;
    }

    public void setCount(int count) {

        this.count = count;
    }

    public void setStartIndex(int startIndex) {

        this.startIndex = startIndex;
    }
}
