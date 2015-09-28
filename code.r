# Code
# stringr is needed for basic str_trim function. It needs to be installed:
# install.packages("stringr")
# install.packages("doBy")
library(doBy)

# precipitation data. This gives us wban/time->precip data.
raw_precip <- read.table("data/201505precip.txt", header=TRUE, sep=",", strip.white=TRUE);
# nrow(raw_precip) = 1717152

# 1 is midnight, 2 is 1 AM, 7 is 6 AM, 8 is 7 AM
# The instructions say to ignore data from 12-7. Presumably, 7:01 data is ok.
# Allow any data from 7AM onwards.
filtered_precip <- subset(raw_precip, Precipitation != "" & Precipitation != "T" & Hour >= 8)
# nrow(filtered_precip) = 63054

transformed_precip <- data.frame(wban=filtered_precip$Wban,
  precip=as.numeric(as.character(filtered_precip$Precipitation)))

agg_precip <- aggregate(precip ~ wban, transformed_precip, sum)
# nrow(agg_precip) = 1959

# stations
raw_stations <- read.table("data/201505station.txt", header=TRUE,
  sep="|", strip.white=TRUE, quote="", comment.char="");
# nrow(raw_stations) = 2553

transformed_stations <- data.frame(
  wban=raw_stations$WBAN,
  city=tolower(raw_stations$Name),
  state=tolower(raw_stations$State))

raw_loc_and_precip <- merge(x=agg_precip, y=transformed_stations, by="wban")
# nrow(raw_loc_and_precip) = 1959

loc_and_precip <- aggregate(precip ~ city + state, raw_loc_and_precip, sum)
# nrow(loc_and_precip) = 1755

# msa populations: population by city/state.
raw_msa_pop <- read.table("data/cph-msa-populations-cleaned.tsv", header=TRUE, sep="\t", strip.white=TRUE, quote="");
# nrow(raw_msa_pop) = 929

msa_pop <- data.frame(
  city=tolower(stringr::str_trim(sub("(.*),(.*)", "\\1", raw_msa_pop$msa))),
  state=tolower(stringr::str_trim(sub("(.*),(.*)", "\\2", raw_msa_pop$msa))),
  population=as.numeric(gsub(",", "", as.character(raw_msa_pop$population))));

loc_pop_precip_all <- merge(x=loc_and_precip, y=msa_pop, by=c("city", "state"), all=TRUE)

loc_pop_precip <- merge(x=loc_and_precip, y=msa_pop, by=c("city", "state"))
# nrow(loc_pop_precip) = 465

loc_wetness <- transform(loc_pop_precip, wetness=population*precip)

sorted_loc_wetness <- orderBy(~ -wetness, loc_wetness)

write.table(sorted_loc_wetness, "data/r_results.csv", sep=",", row.names=FALSE)

# wban data: not used
# wban <- read.table("wbanmasterlist.psv", header=TRUE, sep="|", strip.white=TRUE);
#head(wban)
