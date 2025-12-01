classdef CellXSeedValidator
    %CELLXSEEDVALIDATOR Summary of this class goes here
    %   Detailed explanation goes here
    
    properties
        config;
        seeds;
        % the dimension of the raw input image (without extension)
        % required to detect the seeds on the boundary
        dim;
    end
    
    methods
        
        function this = CellXSeedValidator(config, seeds, imageDimension)
            this.config = config;
            this.seeds  = seeds;
            this.dim    = imageDimension;
        end
        
        
        function run(this)
            fprintf('   Validating cells ...\n');
            % checks and transforms the seed coordinates to image
            % coordinates
            fprintf('    Detecting cells on image border ...\n');
            this.checkImageBoundaryContact();
            fprintf('    Checking minor axis growth ...\n');
            this.checkMinorAxixsLength();
            
            
            
            thr = this.config.membraneConvolutionThresholdFraction*this.config.membraneReferenceCorrelationValue;
            for i = 1:numel(this.seeds)
                s = this.seeds(i);
                
                if s.isInvalid()
                    continue;
                end
                this.seeds(i).fracOfGoodMemPixels = sum(s.perimeterPixelConvolutionValues>thr)/...
                    length(s.perimeterPixelConvolutionValues);
                
            end
            
			% LW PATCH START: Remove Statistics
			% if numel(this.seeds)>30
            %    fprintf('    Checking statistical values ...\n');
            %    this.checkStatisticalValue();
            %else
            %fprintf('    Checking membrane convolution values ...\n');
            %this.checkMembranePixels();
            %end
			% LW PATCH END

            fprintf('   Finished cell validation ...\n');
        end
        
        
        function checkStatisticalValue(this)
            
            seedValidIndx = (vertcat(this.seeds.skipped)==0);
            statFeatsProbsTotAll  = NaN( numel(seedValidIndx) ,1);
            statCCProbsTotAll = NaN( numel(seedValidIndx) ,1);
            % lets do some statistics on the current seeds' results:
            % 1) make the Validator more adaptive and automatic
            % 2) re-run the segmentation of some seeds?
            
            popCellArea=zeros(numel(this.seeds),1);
            popCellEcc = zeros(numel(this.seeds),1);
            popConvValMean = zeros(numel(this.seeds),1);
            popConvValStd = zeros(numel(this.seeds),1);
            popPerimeter = zeros(numel(this.seeds),1);
            popEquivPerimeter = zeros(numel(this.seeds),1);
            for ns=1:numel(this.seeds)
                if(this.seeds(ns).isInvalid())
                    continue;
                end
                popPerimeter(ns,1)= numel(this.seeds(ns).perimeterPixelListLindx);
                popEquivPerimeter(ns,1)= pi*this.seeds(ns).equivDiameter;
                popCellArea(ns,1)= numel(this.seeds(ns).cellPixelListLindx);
                popCellEcc(ns,1) = this.seeds(ns).eccentricity;
                popConvValMean(ns,1) = mean(this.seeds(ns).perimeterPixelConvolutionValues);
                popConvValStd(ns,1) = std(this.seeds(ns).perimeterPixelConvolutionValues);
            end
            popCVcc =  popConvValStd./popConvValMean;
            
            
            membraneReferenceCorrelationValue = median(popConvValMean(seedValidIndx));
            cvFactors = [1 1.5 2 2.5 3];
            popFractionOfGoodPixels = zeros(numel(this.seeds),numel(cvFactors));
            for cvfc = 1:numel(cvFactors)
                cvf = cvFactors(cvfc);
                membraneCorrelationValueThresh = membraneReferenceCorrelationValue-cvf*median(popConvValStd(seedValidIndx));
                thr = membraneCorrelationValueThresh;
                for ns=1:numel(this.seeds)
                    popFractionOfGoodPixels(ns,cvfc) = sum(this.seeds(ns).perimeterPixelConvolutionValues>thr)/...
                        length(this.seeds(ns).perimeterPixelConvolutionValues);
                end
            end
            
            % Estimate the population distribution characteristics
            shapeMeanValue =  mean( popCellArea(seedValidIndx));
            shapeStdValue = std(popCellArea(seedValidIndx));
            
            popCellAreaCorrected = popCellArea;
            shapeCorrIndx = find(popCellArea>shapeMeanValue);
            popCellAreaCorrected(shapeCorrIndx) = shapeMeanValue;
            
            popExpectedCCMeanValue =  mean( popFractionOfGoodPixels ,2);
            ccMeanValue = mean( popExpectedCCMeanValue(seedValidIndx)  );
            ccStdValue = std( popExpectedCCMeanValue(seedValidIndx)  ) ;
            
            popExpectedCCMeanValueCorrected =  popExpectedCCMeanValue;
            ccCorrIndx = find(popExpectedCCMeanValue>ccMeanValue);
            popExpectedCCMeanValueCorrected(ccCorrIndx) = ccMeanValue;
           
             
             %---remove outliers based on area  and CC ( below the mean)
            statFeatsAllSeeds = [popExpectedCCMeanValueCorrected, popCellAreaCorrected];
            statFeats =  statFeatsAllSeeds(seedValidIndx,:);
            statFeatsNorm =zeros(size(statFeats));
            meanStatFeats = [ccMeanValue,shapeMeanValue];
            stdStatFeats = [ccStdValue,shapeStdValue];
            for ft=1:size(statFeats,2)
                statFeatsNorm(:,ft) = ( statFeats(:,ft) - meanStatFeats(ft) )/stdStatFeats(ft);
            end   
            
            MDdist = zeros(size(statFeatsNorm,1),1);
            for np=1:size(statFeatsNorm,1);
                MDdist(np,1) = statFeatsNorm(np,:)*statFeatsNorm(np,:)';
            end
            probF = 1-chi2cdf(MDdist,size(statFeats,2) );
            statFeatsProbsTotAll(seedValidIndx) =  probF;
            
            
            c=0;maxProb=0.8; minProb=0.01;
            probThresh =  minProb + this.config.idPrecisionRate*(maxProb-minProb);
            for ns=1:numel(this.seeds)
                this.seeds(ns).probBeingValid =  statFeatsProbsTotAll(ns);
                if( this.seeds(ns).isInvalid())
                    continue;
                end
                if(this.seeds(ns).probBeingValid<probThresh)
                  fprintf('Skipped %d\n',ns);
                  seedValidIndx(ns)=0;
                  this.seeds(ns).setSkipped(8, 'Statistical outlier');
                  c = c+1;
                end
            end      
                               
        end
        
        
        
        %
        % check membrane pixel convolution values
        %
        function checkMembranePixels(this)
            
            thr = this.config.membraneConvolutionThresholdFraction*this.config.membraneReferenceCorrelationValue;
            
            fprintf('      Membrane pixel convolution value threshold: %f\n', thr);
            c = 0;
            for i = 1:numel(this.seeds)
                s = this.seeds(i);
                
                if s.isInvalid()
                    continue;
                end
                
                fractionOfGoodPixels = sum(s.perimeterPixelConvolutionValues>thr)/...
                    length(s.perimeterPixelConvolutionValues);
                fprintf('      Seed %d, fraction of good membrane pixels: %f\n', i, fractionOfGoodPixels);
                this.seeds(i).fracOfGoodMemPixels = fractionOfGoodPixels;
                if fractionOfGoodPixels < this.config.requiredFractionOfAcceptedMembranePixels
                    fprintf('Skipped %d\n',i);
                    s.setSkipped(4, 'Not enough high value membrane pixels');
                    c = c + 1;
                end
            end
            if c ~=0 
                fprintf('   -> Removed %d seed(s) because of weak membrane pixels\n', c);
            end
            
        end
        
        
        
        %
        % compare the minor axis length and hough transform radius
        %
        function checkMinorAxixsLength(this)
            c=0;
            for i=1:numel(this.seeds)
                s =this.seeds(i);
                if(s.isInvalid())
                    continue;
                end
                
                ratio = s.minorAxisLength/(2*s.houghRadius);
                
                if( ratio > this.config.maximumMinorAxisLengthHoughRadiusRatio )
                    s.setSkipped(3, sprintf('Minor axis length/(2*Hough radius)=%f > %f', ratio, this.config.maximumMinorAxisLengthHoughRadiusRatio));
                    c = c+1;
                end           
            end
            if(c~=0)
                fprintf('   -> Removed %d seed(s) because of minor axis growth\n', c);
            end
        end
        
        
        % remove seeds that hit the image boundary
        function checkImageBoundaryContact(this)
            c=0;
            for i=1:numel(this.seeds)
                s =this.seeds(i);
                if(s.isInvalid())
                    continue;
                end
                
                s.transformBoundingBoxToImageCoordinates(this.config.maximumCellLength);
                
                bb = s.boundingBox;
                
                d1x = bb(1);
                d1y = bb(2);
                d2x = this.dim(2) - (bb(1)+bb(3));
                d2y = this.dim(1) - (bb(2)+bb(4));
                
                
                minD = this.config.requiredDistanceToImageBoundary;
                isValid =  d1x >= minD & d1x > 0 & ...
                    d1y >= minD & d1y > 0 & ...
                    d2x >= minD & d2x > 0 & ...
                    d2y >= minD & d2y > 0;
                
                if ~isValid
                    s.setSkipped(2, 'Seed hits image boundary');
                    c = c+1;
                else
                    s.transformToImageCoordinates(this.dim, this.config.maximumCellLength);
                end          
            end
            
            if(c~=0)
                fprintf('   -> Removed %d seed(s) on the image boundary\n', c);
            end
        end
        

    end
    
end

