#' Find the change periods in a time-series of data
#' 
#' Takes in a vector of numeric values and returns the change periods of the vector
#' @param x A vector of numeric values
#' @return The identified change periods
#' @export  
changeP <- function(xVect) {
  library(pracma)
  library(signal)
  library(ggplot2)
  xVect <- as.numeric(xVect)
  lastValVec = as.vector(rep(xVect[length(xVect)],times = 100))
  firstValVec = as.vector(rep(xVect[1],times = 100))
  extendX = c(firstValVec,xVect)
  # Step 1: Pass the raw data through a two sided low pass filter (LPF)
  bf <- butter(2, 1/30, type="low")
  filteredX <- filter(bf,extendX)
  filteredX <- filteredX[100:(length(filteredX))]
  
  # Step 2: Perform a Diff on the filtered data
  diffX <- diff(filteredX)
  # Step 3: Pass the diff result through a second two sided LPF
  lastValVec = as.vector(rep(diffX[length(diffX)],times = 100))
  firstValVec = as.vector(rep(diffX[1],times = 100))
  diffX = c(firstValVec,diffX)
  diffX = c(diffX,lastValVec)
  bf2 <- butter(2, 1/30, type="low")
  filteredDiffX <- filter(bf2, diffX)
  filteredDiffX <- filteredDiffX[100:(length(filteredDiffX)-100)]
  # Step 4: Split data into change ups and change down
  # Do this by flattening opposite changes to 0 in respective series
  # Down changes series is inverted to enable peak identification
  changeUp <- filteredDiffX
  for(i in 1:length(changeUp)){
    if(changeUp[i] < 0) changeUp[i] = 0
  }
  
  changeDown <- filteredDiffX
  for(i in 1:length(changeDown)){
    if(changeDown[i] > 0) changeDown[i] = 0
  }
  changeDown <- changeDown * -1
  
  # Step 5: Find a peak threshold
  m1 <- mean(abs(filteredDiffX))
  mPeak = m1 * 2;

  # Step 6; Find the peaks in the up changes & down changes
  peaksUp <- findpeaks(changeUp[1:length(changeUp)], minpeakheight=mPeak )
  peaksDown <- findpeaks(changeDown[1:length(changeDown)], minpeakheight=mPeak )
  
  
  # Step 7: Now find the periods around the peaks
  pUps <-periodDec(peaksUp[,2],changeUp,m1*.1)
  pDowns <- periodDec(peaksDown[,2],changeDown,m1*.1)
  
  return(list("Ups" = pUps, "Downs"=pDowns));
}

# This function is used to return the periods, i.e. when the change started and
# when the change ended around an identified set of peaks (the middle of the change period)
# x is the peaks location, y is the timeseries of the difference (i.e. the timeseries of changes)
# m is the point below which the change period is considered over (i.e. a change so small as to be ignored).
periodDec <- function(x,y,m) {
  pUps <- x;
  changeUp <- y;
  periodsUp <- matrix(0,1,2)
  periodsUpStart <- as.vector(x=0,mode="numeric")
  lastPeakStart = 0;
  for(i in 1: length(pUps)){
    
    if(length(periodsUpStart) > 0){
      lastPeakStart = periodsUpStart[length(periodsUpStart)];
    }
    
    thisPeak <- pUps[i]
    #Find the start of the peak
    j <- thisPeak - 1
    while(changeUp[j] > m && j > 1){
      j = j - 1;
    }
    
    theStart = j;
    
    if(theStart > lastPeakStart){
      #Find the end of the peak
      j <- thisPeak + 1
      while(changeUp[j] > m && j <= length(changeUp)){
        j = j + 1;
      }
      theEnd = j;
      if(theStart < 170){
        theStart = 171
      }
      if(theEnd > theStart){
        periodsUpStart = cbind(periodsUpStart,theStart)
        theP <- c(theStart, theEnd)
        periodsUp <- rbind(periodsUp, theP)
      }
    }
  }
  
  periodsUp = periodsUp[-1,]
  
  return(periodsUp)
}