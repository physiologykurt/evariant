# code
import pandas as pd
import re

raw_precip = pd.read_csv("data/201505precip.txt", sep=",");
# len(raw_precip) = 1717152

# This is necessary for whitespacing stripping.
stripped_precip = pd.DataFrame({
  "wban": [x for x in raw_precip["Wban"]],
  "hour": [x for x in raw_precip["Hour"]],
  "precip": [x.strip() for x in raw_precip["Precipitation"]]
});

filtered_precip = stripped_precip[
    (stripped_precip["precip"] != "") &
    (stripped_precip["precip"] != "T") &
    (stripped_precip["hour"] >= 8)];
# len(filtered_precip) = 63054

transformed_precip = pd.DataFrame({
  "wban": filtered_precip["wban"],
  "precip": [float(x) for x in filtered_precip["precip"]]
});

agg_precip = transformed_precip.groupby("wban").sum().reset_index()
# len(agg_precip) = 1959

# stations: wban -> city/state
raw_stations = pd.read_csv("data/201505station.txt", sep="|");

transformed_stations = pd.DataFrame({
  "wban": [x for x in raw_stations["WBAN"]],
  "city": [str(x).strip().lower() for x in raw_stations["Name"]],
  "state": [str(x).strip().lower() for x in raw_stations["State"]]
});

raw_loc_and_precip = pd.merge(left=agg_precip, right=transformed_stations, on="wban")
# len(raw_loc_and_precip) = 1959

del raw_loc_and_precip["wban"]

loc_and_precip = raw_loc_and_precip.groupby(["city", "state"]).sum().reset_index()
# len(loc_and_precip) = 1755

raw_msa_pop = pd.read_csv("data/cph-msa-populations-cleaned.tsv", sep="\t");
# len(raw_msa_pop) = 929

msa_pop = pd.DataFrame({
  "city": [re.sub(r"(.*),(.*)", r"\1", x).strip().lower() for x in raw_msa_pop["msa"]],
  "state": [re.sub(r"(.*),(.*)", r"\2", x).strip().lower() for x in raw_msa_pop["msa"]],
  "population": [int(re.sub(r",", r"", x)) for x in raw_msa_pop["population"]]
});

loc_pop_precip = pd.merge(left=loc_and_precip, right=msa_pop, on=["city", "state"])
# len(loc_pop_precip) = 465

loc_wetness = pd.DataFrame({
  "city": loc_pop_precip["city"],
  "state": loc_pop_precip["state"],
  "precip": loc_pop_precip["precip"],
  "population": loc_pop_precip["population"],
  "wetness": loc_pop_precip["precip"] * loc_pop_precip["population"]
});

sorted_loc_wetness = loc_wetness.sort("wetness", ascending=False)

sorted_loc_wetness.to_csv("data/py_results.csv", index=False)
