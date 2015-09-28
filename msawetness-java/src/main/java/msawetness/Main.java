package msawetness;

import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main {
    // Ideally we'd have Scala case classes or Python namedtuples or Java 10 value types for this.
    public static class MsaWetnessTuple {
        public MsaWetnessTuple(String city, String state, double precip, int population, double wetness) {
            this.city = city;
            this.state = state;
            this.precip = precip;
            this.population = population;
            this.wetness = wetness;
        }

        public final String city;
        public final String state;
        public final double precip;
        public final int population;
        public final double wetness;
    }

    public static void main(String[] args) throws IOException {
        Path dataDir = Paths.get("../data");
        if (!Files.isDirectory(dataDir)) {
            System.out.println(String.format("Can't find data directory at %s", dataDir.toString()));
            return;
        }

        Path precipFilePath = dataDir.resolve("201505precip.txt");
        Path stationFilePath = dataDir.resolve("201505station.txt");
        Path msaPopulationsFilePath = dataDir.resolve("cph-msa-populations-cleaned.tsv");

        for (Path filePath : Arrays.asList(precipFilePath, stationFilePath, msaPopulationsFilePath)) {
            if (!Files.isRegularFile(filePath)) {
                System.out.println(String.format("Can't find file at %s", filePath.toString()));
                return;
            }
        }

        Map<String, Double> agg_precip = parsePrecipitations(precipFilePath);
        System.out.println(String.format("agg_precip.size(): %d", agg_precip.size()));

        Map<String, String> transformed_stations = parseStations(stationFilePath);
        System.out.println(String.format("transformed_stations.size(): %d", transformed_stations.size()));

        Map<String, Integer> msa_pop = parseMsaPopulations(msaPopulationsFilePath);
        System.out.println(String.format("msa_pop.size(): %d", msa_pop.size()));

        Map<String, Double> loc_and_precip = mergeWbanPrecipWithLocations(agg_precip, transformed_stations);
        System.out.println(String.format("loc_and_precip.size(): %d", loc_and_precip.size()));

        List<MsaWetnessTuple> loc_wetness = mergeMsaPopulations(loc_and_precip, msa_pop);
        System.out.println(String.format("loc_wetness.size(): %d", loc_wetness.size()));

        sortAndWriteOutWetnessRecords(loc_wetness, dataDir.resolve("java_results.csv"));
        System.out.println("done!");
    }

    // wban -> precipitation
    public static Map<String, Double> parsePrecipitations(Path precipFilePath) throws IOException {
        Map<String, Double> map = new HashMap<>();

        List<String[]> raw_precip = readAllCsv(precipFilePath, ',');
        System.out.println(String.format("raw_precip.size(): %d", raw_precip.size()));

        for (String[] row : raw_precip) {
            int wban_id_int = Integer.parseInt(row[0].trim());
            String wban_id = Integer.toString(wban_id_int);
            int hour = Integer.parseInt(row[2].trim());
            String precipitationString = row[3].trim();

            if (!precipitationString.isEmpty() && !precipitationString.equals("T") && hour >= 8) {
                double precipitation = Double.parseDouble(precipitationString);

                map.put(wban_id, map.getOrDefault(wban_id, 0.0) + precipitation);
            }
        }

        return map;
    }

    // wban -> city/state
    public static Map<String, String> parseStations(Path stationFilePath) throws IOException {
        Map<String, String> map = new HashMap<>();

        List<String[]> raw_stations = readAllCsv(stationFilePath, '|');
        System.out.println(String.format("raw_stations.size(): %d", raw_stations.size()));

        for (String[] row : raw_stations) {
            String wban_raw = row[0].trim();
            if (!wban_raw.isEmpty()) {
                int wban_id_int = Integer.parseInt(row[0].trim());
                String wban_id = Integer.toString(wban_id_int);

                String city = row[6].toLowerCase().trim();
                String state = row[7].toLowerCase().trim();
                String cityState = city + ", " + state;

                map.put(wban_id, cityState);
            }
        }

        return map;
    }

    // city/state -> pop
    public static Map<String, Integer> parseMsaPopulations(Path msaPopulationsFilePath) throws IOException {
        Map<String, Integer> map = new HashMap<>();

        List<String[]> raw_msa_pop = readAllCsv(msaPopulationsFilePath, '\t');
        System.out.println(String.format("raw_msa_pop.size(): %d", raw_msa_pop.size()));

        for (String[] row : raw_msa_pop) {
            String cityState = row[0].toLowerCase();
            int population = Integer.parseInt(row[1].replace(",", ""));
            map.put(cityState, population);
        }

        return map;
    }

    public static Map<String, Double> mergeWbanPrecipWithLocations(Map<String, Double> agg_precip,
                                                                   Map<String, String> transformed_stations) {
        Map<String, Double> map = new HashMap<>();

        for (Map.Entry<String, Double> aggEntry : agg_precip.entrySet()) {
            String wban_id = aggEntry.getKey();
            Double precip = aggEntry.getValue();
            String cityState = transformed_stations.get(wban_id);
            if (cityState != null) {
                map.put(cityState, map.getOrDefault(cityState, 0.0) + precip);
            }
        }

        return map;
    }

    public static List<MsaWetnessTuple> mergeMsaPopulations(Map<String, Double> loc_and_precip,
                                                            Map<String, Integer> msa_pop) {
        List<MsaWetnessTuple> list = new ArrayList<>();

        Pattern cityStatePattern = Pattern.compile("(.*),(.*)");

        for (Map.Entry<String, Double> stringDoubleEntry : loc_and_precip.entrySet()) {
            String cityState = stringDoubleEntry.getKey();
            double precip = stringDoubleEntry.getValue();

            Integer populationOptional = msa_pop.get(cityState);
            if (populationOptional != null) {
                Matcher m = cityStatePattern.matcher(cityState);
                if (m.matches()) {
                    String city = m.group(1).trim();
                    String state = m.group(2).trim();

                    double wetness = populationOptional * precip;

                    list.add(new MsaWetnessTuple(city, state, precip, populationOptional, wetness));
                }
            }
        }

        return list;
    }

    public static void sortAndWriteOutWetnessRecords(List<MsaWetnessTuple> records, Path outputPath) throws IOException {
        DecimalFormat df = new DecimalFormat("0.00");
        List<String[]> formattedRecords = records.stream()
                .sorted((t1, t2) -> -Double.compare(t1.wetness, t2.wetness))
                .map(t -> new String[]{
                        t.city, t.state, df.format(t.precip), Integer.toString(t.population), df.format(t.wetness)
                }).collect(Collectors.toList());

        try (CSVWriter writer = new CSVWriter(Files.newBufferedWriter(outputPath),
                CSVWriter.DEFAULT_SEPARATOR, CSVWriter.NO_QUOTE_CHARACTER)) {
            writer.writeNext(new String[] { "city", "state", "preicp", "population", "wetness" });
            writer.writeAll(formattedRecords);
        }
    }

    public static List<String[]> readAllCsv(Path filePath, char delimiter) throws IOException {
        try (CSVReader reader = new CSVReader(Files.newBufferedReader(filePath), delimiter,
                CSVParser.DEFAULT_QUOTE_CHARACTER, CSVParser.DEFAULT_ESCAPE_CHARACTER, 1)) {
            List<String[]> result = reader.readAll();
            removeEmptyLast(result);
            return result;
        }
    }

    public static void removeEmptyLast(List<String[]> stringList) {
        if (!stringList.isEmpty()) {
            String[] last = stringList.get(stringList.size() - 1);
            if ((last.length == 0) || (last.length == 1 && last[0].isEmpty())) {
                stringList.remove(stringList.size() - 1);
            }
        }
    }
}
