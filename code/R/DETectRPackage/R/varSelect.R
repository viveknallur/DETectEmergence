#' Find the internal:external relationships to monitor
#' 
#' Takes in a vector of numeric values and returns the change periods of the vector
#' @param internal a data frame of the internal variable data
#' @param external a data frame of the external variable data
#' @return relChoosen, a character vector of the relationships choosen
#' @export  
chooseVariables <- function(internal,external){
  library(glmnet)
  # Step 1: Check if nearZeroVar
  internal <- injectNoise(internal)
  external <- injectNoise(external)
  relsChosen <- vector(mode="character")
  # Step 2: Check if variable is random
  internal <- checkRandomness(internal)
  external <- checkRandomness(external)
  if(ncol(internal) > 0 & ncol(external) > 0){
    internal <- convertDF(internal)
    external <- convertDF(external)
    # Step 3: Run the Lasso and choose the relationships
    if(ncol(internal) > 0 & ncol(external) > 0) {
      lassoMatrix <- matrix(0, nrow=ncol(internal),ncol=ncol(external))
      # First internal vs external
      if(ncol(external) > 1){ # check if there is more than one external to choose from. If not, pick it.
        XGroup <- getXGroup(external)
        for(i in 1:ncol(internal)){
          y <- internal[,i]
          lassoRun.cv<-cv.glmnet(XGroup,y)
          lassofit<-glmnet(XGroup,y,alpha=1,nlambda=500)
          lassopred<-predict(lassofit,XGroup,s=lassoRun.cv$lambda.min)
          lassocoef<-predict(lassofit,s=lassoRun.cv$lambda.min,type="coefficients")
          for(j in 1:ncol(external)){
            theIndex = j + 1;
            if(lassocoef[theIndex,1] != 0.0){
              lassoMatrix[i,j] = lassoMatrix[i,j] + 1
            }
          }
        }
      }else{
        for(i in 1:ncol(internal)){
          for(j in 1:ncol(external)){
            lassoMatrix[i,j] = lassoMatrix[i,j] + 1;
          }
        }
      }
      # Now do external vs internal
      if(ncol(internal) > 1){
        XGroup <- getXGroup(internal)
        for(i in 1:ncol(external)){
          y <- external[,i]
          lassoRun.cv<-cv.glmnet(XGroup,y)
          lassofit<-glmnet(XGroup,y,alpha=1,nlambda=500)
          lassopred<-predict(lassofit,XGroup,s=lassoRun.cv$lambda.min)
          lassocoef<-predict(lassofit,s=lassoRun.cv$lambda.min,type="coefficients")
          for(j in 1:ncol(internal)){
            theIndex = j + 1;
            if(lassocoef[theIndex,1] != 0.0){
              lassoMatrix[j,i] = lassoMatrix[j,i] + 1
            }
          }
        }
      }else{
        for(i in 1:ncol(internal)){
          for(j in 1:ncol(external)){
            lassoMatrix[i,j] = lassoMatrix[i,j] + 1;
          }
        }
      }
      # Final Step is to see what variables were selected both forward and backwards.
      for(i in 1:ncol(internal)){
        for(j in 1:ncol(external)){
          if(lassoMatrix[i,j] == 2){
            relName = paste(names(internal)[i], names(external)[j], sep = ":")
            relsChosen = cbind(relsChosen,relName)
          }
        }
      }
    }
  }
  return(relsChosen)
}

#' Inject random noise into a vector
#' 
#' Takes in a vector of numeric values and returns the vector with random noise added to each
#' @param x a vector of numeric values
#' @return the vector with noise added
#' @export
injectNoiseVector <- function(x){
  theNoise = runif(n = length(x), min = -0.05, max = 0.05)
  newVextor = x + theNoise
  return(newVextor)
}

#' Return a random number i.e. error
#' 
#' @return a random number representing a small error.
#' @export
returnARandom <- function(){
  theNoise = runif(n = 1, min = -0.05, max = 0.05)
  return(theNoise)
}

#' Return a dataframe plus random noise
#' 
#' @return a random a dataframe plus random noise
#' @export
injectNoise <- function(x){
  rows <- nrow(x)
  cols <- ncol(x)
  theNoise = matrix(runif(cols*rows, min = -0.05, max = 0.05), ncol=cols) 
  noisyX = x + theNoise
  
  return(noisyX)
}

# Returns all columns except the first column in a dataframe
getXGroup <- function(x) {
  XGroup <- x[,1]
  if(ncol(x) > 1){
    for(i in 2:ncol(x)){
      XGroup <- cbind(XGroup,x[,i])
    }
  }
  return (XGroup)
}

# This function checks whether a vector is composed of just random noise/data
checkRandomness <- function(x){
  colsToRemove <- vector(mode = "numeric")
  for(j in 1:ncol(x)){
    h <- hist(x[,j], breaks = 50, plot = FALSE)
    counts <- h$counts
    chiTest <- chisq.test(counts)
    if(chiTest$p.value >= 0.05){
      colsToRemove <- cbind(colsToRemove,j)
    }
  }
  if(length(colsToRemove > 0)){
    newNames <- names(x)[-colsToRemove]
    x <- as.data.frame(x[,-colsToRemove])
    colnames(x) <- newNames
  }
  return(x)
}

# This function is used to standardise the values in a dataframe...ie. return a dataframe
# where the value in each column has had the mean subtracted and been divided by the 
# standard deviation
convertDF <- function(x){
  converted <- x
  for (i in 1:ncol(x)){
    meanX <- mean(x[,i])
    sdX <- sd(x[,i])
    x[,i] = (x[,i] - meanX)/sdX
    converted[,i] = x[,i]
  }
  return(converted)
}