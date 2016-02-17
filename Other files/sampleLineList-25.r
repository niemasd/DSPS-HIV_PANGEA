#This code takes a while to run, as it tracks when everyone in the simulation lives,
#dies, is infected, recovers... etc. 
#You will need to change paths and file names appropriately!
#Run as a block up to about line 168, then run in blocks/lines to ensure
#sampling is progressing as expected

base<-"C:\\Users\\Room65-Admin\\Desktop\\Emma\\Jan-15-Release\\Original Runs\\acuteLow\\" #input location
output <- "C:\\Users\\Room65-Admin\\Desktop\\Emma\\Jan-15-Release\\Sampling3\\"
folder <- "acuteLow_noART_mig" #run that contains the .csv output file from DSPS
description <- "acuteLow-noART-20mig" #output file base

sampPercent <- 0.25  ##what proportion of actively infected to sample 
timeStart <- 40  # >=
timeEnd <- 45   # <  

#NOW This is a bit complicated. We need to know ID of everyone alive at begin of 
#the simulation. From when we generated the XML file, we know we start with
#3000 2-person households (1500 high, 1500 low), and 1 200 person SW deme
#(we will not track villages as we won't sample them, and nothing happens to them)
#This isnt the total number in the XML file, but the total number that are ON at the
#start of the simulation!
#If running a simulation exactly like the PANGEA ones, these numbers will be right
#Otherwise change them to match your own numbers

#So, we have 3001 houses - 3000 'normal' and 1 SW.

numHouses <- 3001
neibGroup <- c("High", "Low", "SW")
numYears <- 70  #number of years simulation runs

numSexWork <- 200
numStartDeme <- 3000  #how many of the starting demes have normal individuals in them
startIndivs <- c(0:(numStartDeme*2-1))  #number of people in normal demes, unknown IDs
#^these are the people alive at start in normal demes, these will be their ID numbers

lastNormIndiv <- startIndivs[length(startIndivs)] #this is the ID of the last 'normal' person


#####################################
#####################################

setwd(paste(base,folder,sep=""))
dir.create(file.path(output,folder))

outputFolder <- paste(output,folder,"\\",sep="")

logT <- read.csv("simpleSIR_1_infectionEventLog.csv",
		header=TRUE, skip=2, as.is=TRUE)	
logT2 <- logT
logT3 <- logT

alive <- c()
infected <- c()

#get number of sex workers:
swDeme <- gsub("-[0-9]+-[A-Z]+", "", logT2[which(logT2[,1]=="INFECTION")[1],4] )
swkrs <- c()
for(i in 1:numSexWork){
	swkrs <- c(swkrs, paste(swDeme,"-",(lastNormIndiv+i),"-FEMALE", sep=""))
}

alive <- swkrs
aliveUnknown <- startIndivs
numAlive <- length(alive)+length(aliveUnknown)

#record who was first infected:
firstInfect <- logT2[which(logT2[,1]=="INFECTION")[1],4] 

actionTime <- logT2$ActionTime
logT2 <- logT2[,c(1,4,5)]
logIt <- logT2

init <- firstInfect
infected <- c(infected, init)
prop <- c()
numAlive <- c()
numInfected <- c()
		
treated <- c()
numTreated <- c()
		
recovered <- c()
migDescendInf <- c()
numMigDescendInf <- c()
numRecovered <- c()
numSusceptible <- c()
numDied <- 0
migDescendRecov <- c()
numMigDescendRecov <- c()
intros <- c()

infPeriod <- c()

for(i in 1:nrow(logIt)){

	mig <- grepl("MIG",logIt[i,3])
	indiv <- gsub("-MIG", "", logIt[i,3])
	intro <- 0
	
	if(logIt[i,1]=="INFECTION"){
		if(!grepl("Vil",indiv)) { #dont do anything if it's a villager.
			if(!indiv%in%alive) {  #if not in alive, this is first event of this person!
				aliveUnknown <- aliveUnknown[1:(length(aliveUnknown)-1)]
				alive <- c(alive, indiv)
			}
			infected <- c(infected, indiv)
			if(mig) {
				migDescendInf <- c(migDescendInf, indiv) }
		} 
		if(grepl("Vil",logIt[i,2])) { #if the infector is a villager
			intro <- 1
		}		
		
	} else if(logIt[i,1]=="BIRTH"){
		alive <- c(alive, indiv)
		
	} else if(logIt[i,1]=="DEATH"){
		if(!indiv%in%alive) {  #if not in alive, this is first event of this person!
			aliveUnknown <- aliveUnknown[1:(length(aliveUnknown)-1)]
		}
	
		alive <- alive[! alive%in%indiv ]
		infected <- infected[! infected%in%indiv ]
		recovered <- recovered[! recovered%in%indiv ]
		treated <- treated[! treated%in%indiv ]
		if(mig) {
			migDescendInf <- migDescendInf[! migDescendInf%in%indiv]
			migDescendRecov <- migDescendRecov[! migDescendRecov%in%indiv]
		}
		numDied <- numDied+1
	
	} else if(logIt[i,1]=="RECOVERY"){ 	
		infected <- infected[! infected%in%indiv ]
		treated <- treated[! treated%in%indiv ] 
		recovered <- c(recovered, indiv)
		if(mig) {
			migDescendInf <- migDescendInf[! migDescendInf%in%indiv]
			migDescendRecov <- c(migDescendRecov, indiv)	
		}
		
	} else if(logIt[i,1]=="TREATMENT"){
		treated <- c(treated, logIt[i,3])
		
	} #else is sampling
	
	sus <- alive[! alive%in%recovered]
	sus <- sus[! sus%in%infected]
	numSusceptible <- c(numSusceptible, (length(sus)+length(aliveUnknown)) )
	
	#print(paste("Just processed ",logIt[i,1],": ", logIt[i,2], "and", logIt[i,3]))

	prop <- c(prop, length(infected)/length(alive))
	numInfected <- c(numInfected, length(infected))
	numTreated <- c(numTreated, length(treated))
	
	numAlive <- c(numAlive, length(alive)+length(aliveUnknown))

	numRecovered <- c(numRecovered, length(recovered))
	numMigDescendInf <- c(numMigDescendInf, length(migDescendInf) )
	numMigDescendRecov <- c(numMigDescendRecov, length(migDescendRecov) )
	
	if(logT3$ActionTime[i] >= timeStart & logT3$ActionTime[i] <timeEnd ){
		infPeriod <- c(infPeriod, infected)
		infPeriod <- unique(infPeriod)  
	}
}

#######################Run up to here, then run the next bit line by line,
### ensure sampling is going as expected


#this samples just 25% of the infecteds. nothing fancy.

#SAMPLES ONLY THREE MONTHS AFTER INFECTION!!!!
#so excludes all those for which we cannot get a sample, because there is not three months
#between infection and (death/recovery)

#samples new people to make up for this, and sees if they can be sampled instead

periodSampNum <- round(length(infPeriod) * sampPercent)

#############
##get line list original
logLi <- logT3
logLi <- logLi[which(logLi$EventType == "INFECTION"),]
logLi <- logLi[, c(1,3,4,5)]
#add first line so that we have initial infection
logLi <- rbind(c("INFECTION", 0.0, NA, logLi[1,3]), logLi)

#sample
toSamp <- sample(infPeriod, periodSampNum, replace=F)

toSampOrig <- toSamp
unSampled <- infPeriod[which(!infPeriod%in%toSamp)]

#######
###get sample times
realSamp <- c()
times <- c()
probs <- c()
added <- c()

i <- 1
go <- TRUE
while(go){
	ind <- toSamp[i]
	
	temp <- logT3[grep(ind, logT3$ToDeme.ToHost),]
	infT <- temp$ActionTime[which(temp$EventType == "INFECTION")]
	stpTs <- temp$ActionTime[which(temp$EventType == "DEATH" | temp$EventType=="RECOVERY")]
	if(length(stpTs)==0){
#		 print(paste("no stoptime!",i)) 
		 stpT <- 70
	} else {	
		stpT <- min(stpTs)
	}
	
	if(length(infT) > 1) print(paste("infT >1!",i))
	if(length(stpT) > 1) print(paste("stpT >1!",i))
	
	#make it so sampling can only happen 3 months after infection
	threeM <- 0.25
	
	strtSamp <- max(timeStart,(infT+threeM))
	endSamp <- min(timeEnd, stpT)
	
	if(strtSamp >= endSamp){
		print(paste("PROBLEM!: no time to sample!",i))
		probs <- c(probs,i)
		newS <- sample(unSampled,1,replace=F)
		toSamp[i] <- newS
		added <- c(added, newS)
		unSampled <- unSampled[which(!unSampled%in%newS)]
		
	} else {
	
		tim <- runif(1, strtSamp, endSamp)
		
		while(tim <= (infT+threeM) | tim >= stpT){
			tim <- runif(1, strtSamp, endSamp)
		}
		
		realSamp <- c(realSamp, temp$ToDeme.ToHost[1])
		times <- c(times, tim)
		
		i <- i+1
		if(i>length(toSamp)) {go <- FALSE }
	}
	
}

length(probs) # this many cannot be sampled after at least 3 months!

#note how many you are actually sampling out of the number expected to be sampled


sampLines <- cbind("SAMPLING", times, realSamp, realSamp)

#lets save the sample names and times somewhere..
write.csv(sampLines[,c(2,3)], paste(outputFolder, "sampled_25p.csv",sep=""),
	row.names=FALSE, quote=FALSE)
	

##########################
###add samples to line list

#~~~~~~~~~~~~~~~~~~~~~
####add 25 p to list
sampLines <- as.data.frame(sampLines)
colnames(sampLines) <- colnames(logLi)
logLi2 <- rbind(logLi, sampLines)

#arrange by time
logLi2$ActionTime <- as.numeric(logLi2$ActionTime)
logLi3 <- logLi2[order(logLi2$ActionTime, decreasing=F),]

#get rid of -MIG endings on some things.
logLi3$FromDeme.FromHost <- gsub("-MIG", "", logLi3$FromDeme.FromHost)
logLi3$ToDeme.ToHost <- gsub("-MIG", "", logLi3$ToDeme.ToHost)

#if groups are used, put out two versions, one with groups and one without
groups <- c("High", "Low", "SW", "Vil")
pc <- sampPercent*100
fname <- paste(description,"_25plineList", sep="")
write.csv(logLi3, paste(outputFolder, fname,".csv",sep=""),
	row.names=FALSE, quote=FALSE)
	
if(length(groups)!=0){
	cleanLog <- logLi3
	for(i in 1:length(groups)){
		cleanLog$FromDeme.FromHost <- sub(groups[i], "", cleanLog$FromDeme.FromHost)
		cleanLog$ToDeme.ToHost <- sub(groups[i], "", cleanLog$ToDeme.ToHost)
	}
}

write.csv(cleanLog, paste(outputFolder, fname,"_noGroup.csv",sep=""),
	row.names=FALSE, quote=FALSE)






