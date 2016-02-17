#This code will generate 2-person high/low risk demes, SW deme, and village demes
#It will generate enough to allow whatever exponential growth you want
#By creating the final number of demes, then turning some 'off' and they will be
#turned on as the program runs to allow more growth. 

#You must decide the end proportions of each type of deme now! 
#As DSPS will turn them on randomly.

numStartDemes <- 3000
numYears <- 70
growthRate <- 0.01
treat <- TRUE
treatTimer <- 40.0
treatPerc <- 0.2
trtP <- "0.5" #treatment parameter to use
#path for actual runs
path <- "\t<parameter id=\"Path\" value=\"C://Users//Room65-Admin//Desktop//Emma//TestExpRuns//\"/>"
#path for eclipse runs
#path <- "\t<parameter id=\"Path\" value=\"test//\"/>"

#make this match wherever your DSPS-HIV will look for this hard-coded file...
#if it doesn't find it, then it will still run anyway
write(numYears, "C:/Users/Room65-Admin/Desktop/Emma/newMaxTime.txt")

sizEachYr <- numStartDemes
toAddEachYr <- c()
st <- numStartDemes
for(i in 1:numYears) {
	ad <- round(st * growthRate)
	st <- st+ad
	sizEachYr <- c(sizEachYr, st)
	toAddEachYr <- c(toAddEachYr, ad)
}

swSizeEachYr <- 200
swToAddEchYr <- c()
swT <- swSizeEachYr
for(i in 1:numYears) {
	ad <- round(swT * growthRate)
	swT <- swT+ad
	swSizeEachYr <- c(swSizeEachYr, swT)
	swToAddEchYr <- c(swToAddEchYr, ad)
}

#This is how your number of ON demes should grow each year:
sizEachYr

#This is how your sex worker deme should grow each year:
swSizeEachYr

#You will need to turn on this many demes by end of sim:
totNumAdd <- sum(toAddEachYr)
totNumAdd

#######
numHouses <- numStartDemes+totNumAdd
#adjust to be even:
if(numHouses%%2 != 0) {
	numHouses <- numHouses+1; totNumAdd <- totNumAdd+1
}
numNormDemes <- numHouses
numInGroup <- c(numHouses/2, numHouses/2, 1, 6) #high, low, SW, vil
numHouses <- sum(numInGroup)

swGroup <- sum(numInGroup[1:3]) #of SW is third in groups!
vilGroup <- (sum(numInGroup[1:3])+1):numHouses

#prob of contact outside of the 2-person deme, for high risk, low risk, SW, and Vil demes
#Risk ofhaving contact within your own deme (your partner) is 1-(all other risks)
#as a table this translates to:
#					Risk of having contact with
#				Own partner	High	Low		SW
#	high risk	(0.5)		0.2		0.1		0.2
#	low risk	(0.8)		0.08	0.05	0.07
#	SW 			(0)			0.8		0.2		0.0
#	Vil			(0)			0.3		0.2		0.5

groupMig <- c("0.2, 0.1, 0.2", "0.08, 0.05, 0.07", "0.8, 0.2, 0.0", "0.3, 0.2, 0.5") #high, low, SW, vil
neibGroup <- c("High", "Low", "SW")
neibGroupList <- c("High", "Low", "SW", "Vil")

#decide how many are off (these will turn on to enable exponential growth):
highs <- 1:numInGroup[1]
lows <- (numInGroup[1]+1):(sum(numInGroup[1:2]))
offNums <- c(sample(lows, totNumAdd/2, replace=F), sample(highs, totNumAdd/2, replace=F) )

if(treat) {
	#decide how many houses to treat....
	toTreat <- sample(numNormDemes, round(numNormDemes*treatPerc), replace=F)
	toTreat <- c(toTreat, swGroup)
}


#outFile <- "C:/Users/Emma/Desktop/demeOut.xml" #for Emma computer
outFile <- "C:/Users/Room65-Admin/Desktop/Emma/demeOut.xml"  #for Sam's computer

#to stop R from getting too bogged down with stuff in memory
writeOutEvery <- 500

nums <- c(1:numHouses)
cumNum <- c()
lastSum <- 0
for(i in 1:length(numInGroup)){
	cumNum <- c(cumNum, numInGroup[i]+lastSum)
	lastSum <- numInGroup[i]+lastSum
}

#########################
#print header stuff
allDeme <- c()

fileHd <- paste("<?xml version=\"1.0\" standalone=\"yes\"?>

<DSPS>
<General>
	\t<parameter id=\"Seed\" value=\"12369\"/>
	",path,"
	\t<parameter id=\"Rootname\" value=\"simpleSIR\"/>
	\t<parameter id=\"Nreps\" value=\"1\"/>
	\t<parameter id=\"Tauleap\" value=\"0\"/>
	\t<parameter id=\"MaxTime\" value=\"",numYears,"\"/> 
	\t<parameter id=\"Heritability\" value=\"1\"/> <!-- turns on using virus/heritability -->  
	\t<parameter id=\"ViralLoad\" value=\"4.5\"/> <!-- specifies initial viral load - only works if heritability also turned on! -->
	\t<parameter id=\"RecordAllEvents\" value=\"false\"/> <!-- defaults to false unless set to true. will record ALL events (success or not)-->
	\t<parameter id=\"OutputTrees\" value=\"Nexus\"/> <!-- defaults to newick unless specified anything else, is then nexus-->
	\t<parameter id=\"Gender\" value=\"true\"/> <!-- defaults to no gender used. If true & gender not specified in each deme, random genders assigned. -->
	\t<parameter id=\"Orientation\" value=\"true\"/> <!-- defaults to no Orientation used (same as bisexual). Can only be true if gender true. If true but not specified, all are bisexual. -->
	\t<parameter id=\"DemeGroups\" value=\"", paste(neibGroupList, collapse=",") ,"\"/> 
	\t<parameter id=\"ExpGrowthRate\" value=\"",growthRate,"\"/>
</General>
<Sampler>
	\t<parameter id=\"SamplerType\" value=\"individualBasedModel.JustBeforeRecoveryTreatmentSampler\"/>
	\t<parameter id=\"justBefore\" value=\"1.0E-16\"/>
</Sampler>
<Demes>", sep="")

write(fileHd, file=outFile)

########################

doingGroup <- 1
for(i in 1:numHouses) {

	trtParam <-""
	if(treat) {
		#see if being treated:
		if(i %in% toTreat){
	trtParam <- paste("\t\t<parameter id=\"TreatmentParameter\" value=\"",trtP,"\"/> \r", sep="")
		}
	}
	
	#see if normal, SW, or Vil
	maxHostParam <- "\t\t<parameter id=\"MaxHostsPerDeme\" value=\"2\"/> \r"
	genderOnlyParam <- ""
	numMaleFemaleParam <- "\t\t<parameter id=\"NumberOfMaleFemaleHosts\" value=\"1,1\"/> \r"
	birthDeathParam <- "\t\t<parameter id=\"BirthDeathParameters\" value=\"0.6,0.01\"/> \r"
	numHostsParam <- "\t\t<parameter id=\"NumberOfHostsPerDeme\" value=\"2\"/> \r"
	infectParam <- "\t\t<parameter id=\"InfectionParameters\" value=\"100,0.8\"/> \r"
	if(i %in% swGroup){
		numMaleFemaleParam <- "\t\t<parameter id=\"NumberOfMaleFemaleHosts\" value=\"0,200\"/> \r"
		maxHostParam <- "\t\t<parameter id=\"MaxHostsPerDeme\" value=\"exp\"/> \r" 
		genderOnlyParam <- "\t\t<parameter id=\"GenderOnly\" value=\"Female\"/> \r"
		numHostsParam <- "\t\t<parameter id=\"NumberOfHostsPerDeme\" value=\"200\"/> \r"
		infectParam <- "\t\t<parameter id=\"InfectionParameters\" value=\"600,0.8\"/> \r"
	} else if(i %in% vilGroup) {
		numMaleFemaleParam <- "\t\t<parameter id=\"NumberOfMaleFemaleHosts\" value=\"1,0\"/> \r"
		maxHostParam <- "\t\t<parameter id=\"MaxHostsPerDeme\" value=\"1\"/> \r" 
		birthDeathParam <- "\t\t<parameter id=\"BirthDeathParameters\" value=\"0,0\"/> \r"
		numHostsParam <- "\t\t<parameter id=\"NumberOfHostsPerDeme\" value=\"1\"/> \r"
		infectParam <- "\t\t<parameter id=\"InfectionParameters\" value=\"12,0\"/> \r"
	}
	
	#see if off:  NORMAL DEMES ONLY!!
	offParam <-""
	if(i %in% offNums){
		offParam <- "\t\t<parameter id=\"DemeStatus\" value=\"off\"/> \r"
		numHostsParam <- "\t\t<parameter id=\"NumberOfHostsPerDeme\" value=\"0\"/> \r"
	}
	

	if(i > cumNum[doingGroup]) {
		doingGroup <- doingGroup+1  }
	
	if(length(neibGroupList)==1){
		curNam <- paste("House", i, sep="")
	} else {
		curNam <- paste("House", neibGroupList[doingGroup], i, sep="")
	}
	dmGr <- paste("\t\t<parameter id=\"DemeGroup\" value=\"", neibGroupList[doingGroup], "\"/> \r", sep="")
	mgP <- paste("\t\t<parameter id=\"MigrationParameters\" value=\"", groupMig[doingGroup], "\"/> \r", sep="")

	
	dem <- paste("\t<Deme> \r",
"\t\t<parameter id=\"DemeUID\" value=\"", i-1, "\"/> \r",
"\t\t<parameter id=\"DemeName\" value=\"", curNam, "\"/> \r",
numMaleFemaleParam,genderOnlyParam,
numHostsParam,
birthDeathParam,
dmGr, mgP, trtParam,
infectParam,
"\t\t<parameter id=\"NeighbourDemeGroups\" value=\"", paste(neibGroup, collapse=","), "\"/> \r",
maxHostParam,offParam,
"\t</Deme> ", sep="")

	if(i%%writeOutEvery == 0){ #dump to file
		allDeme <- paste(allDeme, dem, sep="")
		write(allDeme, file=outFile, append=T)
		allDeme <- c()
		print(paste("Done ",i," demes",sep=""))
		
	} else { #save and keep going
		allDeme <- paste(allDeme, dem, "\r", sep="")
	}
	
}

printWarn <- FALSE

if(i%%writeOutEvery != 0) { #if we didnt dump to file just before exiting loop, do it now
	write(allDeme, file=outFile, append=T)
	allDeme <- c()
	printWarn <- TRUE
}


fileEnd <- "</Demes>
<PopulationStructure>
	\t<parameter id=\"NetworkType\" value=\"FULL\"/> <!-- this is default value - connects all demes -->  
	\t<parameter id=\"ModelType\" value=\"SIRT\"/>
	\t<parameter id=\"BirthDeathType\" value=\"exponential\"/> <!-- defaults to 'growth' if birth death turned on, otherwise specify 'stable' - does noting if birthDeath params are 0 or excluded -->
	\t<parameter id=\"OrientationChoice\" value=\"hetero\"/> 
	\t<parameter id=\"DemeType\" value=\"INFECTION_OVER_NETWORK\"/> \r"
if(treat) {
	fileEnd <- paste(fileEnd, "\t<parameter id=\"TreatmentTimer\" value=\"",treatTimer,"\"/> \r",sep="")
}
	
fileEnd <- paste(fileEnd, "</PopulationStructure>
</DSPS>",sep="")

write(fileEnd, file=outFile, append=T)

#If you see this warning check that there aren't extra blank lines at end of file.
if(printWarn) { print("WARNING: Dumped outside of the 'clean' dumps in the loop. Check for extra line breaks") }

###############

#code you'll need for start of plotting: - leaving out 'Vil'
cat(paste(
"numHouses <- ", numStartDemes+1, "\n",#+1 for SW deme
"numInGroup <- c(", numStartDemes/2,", ", numStartDemes/2,", 1)",  "\n",
"cumNum <- c(", numStartDemes/2,", ", numStartDemes,", ", numStartDemes+1,")",  "\n",
"neibGroup <- c(\"", paste(neibGroup, collapse="\",\""),"\")",  "\n",
"numYears <- ",numYears,"\n",
"numPplInHouse <- c(2,2,200) \n\n\n", sep=""))



