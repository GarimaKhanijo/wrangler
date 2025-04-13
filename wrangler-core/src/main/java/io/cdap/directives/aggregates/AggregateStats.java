package io.cdap.directives.aggregates;

import io.cdap.wrangler.api.*;
import io.cdap.wrangler.api.parser.*;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.executor.ExecutorContext;

import java.util.List;
import java.util.Map;

@Directive(name = "aggregate-stats", usage = "aggregate-stats <sizeCol> <timeCol> <outputSizeCol> <outputTimeCol> [outputSizeUnit] [outputTimeUnit] [aggregationType]")
public class AggregateStats implements Directive {

    private String sizeCol;
    private String timeCol;
    private String outputSizeCol;
    private String outputTimeCol;
    private String sizeUnit = "B";      // Default: bytes
    private String timeUnit = "ms";     // Default: milliseconds
    private String aggregation = "total"; // Default: total

    private transient Store store;

    @Override
    public UsageDefinition define() {
        UsageDefinition.Builder builder = UsageDefinition.builder("aggregate-stats");
        builder.define("sizeCol", TokenType.IDENTIFIER);
        builder.define("timeCol", TokenType.IDENTIFIER);
        builder.define("outputSizeCol", TokenType.IDENTIFIER);
        builder.define("outputTimeCol", TokenType.IDENTIFIER);
        builder.defineOptional("sizeUnit", TokenType.IDENTIFIER);
        builder.defineOptional("timeUnit", TokenType.IDENTIFIER);
        builder.defineOptional("aggregation", TokenType.IDENTIFIER);
        return builder.build();
    }

    @Override
    public void initialize(Arguments arguments) {
        sizeCol = ((Identifier) arguments.value("sizeCol")).value();
        timeCol = ((Identifier) arguments.value("timeCol")).value();
        outputSizeCol = ((Identifier) arguments.value("outputSizeCol")).value();
        outputTimeCol = ((Identifier) arguments.value("outputTimeCol")).value();

        if (arguments.contains("sizeUnit")) {
            sizeUnit = ((Identifier) arguments.value("sizeUnit")).value();
        }
        if (arguments.contains("timeUnit")) {
            timeUnit = ((Identifier) arguments.value("timeUnit")).value();
        }
        if (arguments.contains("aggregation")) {
            aggregation = ((Identifier) arguments.value("aggregation")).value();
        }
    }

    @Override
    public void initialize(ExecutorContext context) {
        this.store = context.getStore(); // use context-wide store
    }

    @Override
    public List<Row> execute(List<Row> rows, ExecutorContext ctx) {
        for (Row row : rows) {
            Object sizeObj = row.getValue(sizeCol);
            Object timeObj = row.getValue(timeCol);

            long sizeInBytes = parseSize(sizeObj);
            long timeInMillis = parseDuration(timeObj);

            // Accumulate totals in store
            store.put("total_bytes", store.getOrDefault("total_bytes", 0L) + sizeInBytes);
            store.put("total_time", store.getOrDefault("total_time", 0L) + timeInMillis);
            store.put("count", store.getOrDefault("count", 0L) + 1);
        }

        // No transformation on each row, return unchanged
        return rows;
    }

    @Override
    public List<Row> finalize(List<Row> rows) {
        long totalBytes = store.getOrDefault("total_bytes", 0L);
        long totalTime = store.getOrDefault("total_time", 0L);
        long count = store.getOrDefault("count", 0L);

        double finalSize = aggregation.equalsIgnoreCase("average") ? totalBytes / (double) count : totalBytes;
        double finalTime = aggregation.equalsIgnoreCase("average") ? totalTime / (double) count : totalTime;

        // Convert units if needed
        finalSize = convertSize(finalSize, sizeUnit);
        finalTime = convertTime(finalTime, timeUnit);

        Row summaryRow = new Row();
        summaryRow.add(outputSizeCol, finalSize);
        summaryRow.add(outputTimeCol, finalTime);

        rows.add(summaryRow);
        return rows;
    }

    // ---------- Helper Methods ----------

    private long parseSize(Object obj) {
        String val = obj.toString().toUpperCase().trim();
        if (val.endsWith("KB")) return (long)(Double.parseDouble(val.replace("KB", "")) * 1024);
        if (val.endsWith("MB")) return (long)(Double.parseDouble(val.replace("MB", "")) * 1024 * 1024);
        if (val.endsWith("GB")) return (long)(Double.parseDouble(val.replace("GB", "")) * 1024 * 1024 * 1024);
        if (val.endsWith("TB")) return (long)(Double.parseDouble(val.replace("TB", "")) * 1024L * 1024L * 1024L * 1024L);
        if (val.endsWith("B")) return Long.parseLong(val.replace("B", ""));
        return Long.parseLong(val); // assume bytes
    }

    private long parseDuration(Object obj) {
        String val = obj.toString().toLowerCase().trim();
        if (val.endsWith("ms")) return (long)(Double.parseDouble(val.replace("ms", "")));
        if (val.endsWith("s")) return (long)(Double.parseDouble(val.replace("s", "")) * 1000);
        if (val.endsWith("min")) return (long)(Double.parseDouble(val.replace("min", "")) * 60000);
        if (val.endsWith("h")) return (long)(Double.parseDouble(val.replace("h", "")) * 3600000);
        return Long.parseLong(val); // assume ms
    }

    private double convertSize(double bytes, String unit) {
        switch (unit.toUpperCase()) {
            case "KB": return bytes / 1024;
            case "MB": return bytes / (1024 * 1024);
            case "GB": return bytes / (1024 * 1024 * 1024);
            case "TB": return bytes / (1024L * 1024L * 1024L * 1024L);
            default: return bytes;
        }
    }

    private double convertTime(double ms, String unit) {
        switch (unit.toLowerCase()) {
            case "s": return ms / 1000;
            case "min": return ms / 60000;
            case "h": return ms / 3600000;
            default: return ms;
        }
    }
}

