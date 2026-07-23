classdef CellXIntensityExtractorYouScope < handle
    %UNTITLED4 Summary of this class goes here
    %   Detailed explanation goes here
    
    properties
        
        %input
        config;
        fileSet;
        detectedCells;
        
        segmentationBinaryMask % temp
        
        %output
        fluoInitialImages;
        flatFieldImages;
        fluoImagesFlatFieldCorrected;
        fluoImagesAlligned;
        alignmentMove;
        fluoImages;
        fluoImagesBackground;
        
    end
    
    methods
        
        
        function obj = CellXIntensityExtractorYouScope(config, fSet, detectedCells, fluoInitialImages)           
            obj.config        = config;
            obj.fileSet       = fSet;
            obj.detectedCells = detectedCells; 
            obj.fluoInitialImages  = fluoInitialImages;
        end
        
        
        function run(this)
            tic;     
            fprintf(' Extracting intensities ...\n');
            % for each intensity image, do an intensity extraction loop
            for fi = 1:this.fileSet.getNumberOfFluoImages()
                
                %--1. load the initial fluo images
%                fluofilename = this.fileSet.getFluoImage(fi);
%                this.fluoInitialImages{fi} = CellXImageIO.loadToGrayScaleImage(fluofilename, this.config.cropRegionBoundary);
                % substitute the one we measure
                this.fluoImages{fi} =this.fluoInitialImages{fi};

                % Use crop when cropRegionBoundary is set      
                if ~isempty(this.config.cropRegionBoundary)
                    this.fluoImages{fi} = imcrop_own(this.fluoImages{fi}, round(this.config.cropRegionBoundary), size(this.fluoImages{fi}, 1), size(this.fluoImages{fi}, 2));
                end
                
                %--2. do the flat-field correction if image exists
                if this.fileSet.hasFlatFieldImage(fi)
                    % load flatfield image
                    %flatfieldfilename = this.config.flatfieldFilenames{fi};
                    %this.flatFieldImages{fi} = this.loadImage(flatfieldfilename);
                    this.flatFieldImages{fi} = ...
                        CellXImageIO.loadToGrayScaleImage( ...
                        this.fileSet.getFlatFieldImage(fi),this.config.cropRegionBoundary );
                    % do the flat-field correction
                    flatfieldCorrectedImage =...
                        CellXImageIO.getFluoFlatFieldCorrection( ...
                        this.flatFieldImages{fi}, ...
                        this.fluoImages{fi}, ...
                        this.config.wiener2params);
                    % store them
                    this.fluoImagesFlatFieldCorrected{fi} = flatfieldCorrectedImage;
                    % substitute the one we measure
                    this.fluoImages{fi} = flatfieldCorrectedImage;
                end
                
                %--3. get the fluo image background value
                % start temp code (the segmentation mask should be ready on the cellXSegmenter)
                SegBinaryMask =zeros(size(this.fluoImages{fi}));
                for nrc = 1:length(this.detectedCells)
                    curPixelList = this.detectedCells(nrc).cellPixelListLindx;
                    SegBinaryMask(curPixelList) = 1;
                end
                SegBinaryMask = logical(SegBinaryMask);
                this.segmentationBinaryMask= SegBinaryMask;
                % end temp code
                fluoBackgroundVals = this.computeFluoBackground(this.fluoImages{fi},~SegBinaryMask);
                this.fluoImagesBackground{fi} = fluoBackgroundVals;
                
                %--4. do the allignment if required
                if this.config.fluoAlignPixelMove>0
                    
                    % extend the image with the background value
                    %extendedfluoImage = ...
                    %    this.extendFluoImage(this.fluoImages{fi},fluoBackgroundVals);
                    extendedfluoImage = CellXImageIO.extendImageWithNoise( ...
                        this.fluoImages{fi}, ...
                        this.config.fluoAlignPixelMove, ...
                        fluoBackgroundVals );
                    % find the optimal pixel move and the aligned fluo
                    % image
                    [this.alignmentMove(fi,:)  this.fluoImagesAlligned{fi}]= ...
                        this.findOptimalPixelMoveAndAlignedImage(extendedfluoImage, SegBinaryMask);
                    
                    % substitute the one we measure
                    this.fluoImages{fi} = this.fluoImagesAlligned{fi};
                end
                
                
                %--5. do the measurements
                this.measureIntensities(fi);
                
            end
            fprintf(' Finished intensity extraction\n');
            t=toc;
            fprintf(' Elapsed time: %4.2fs\n', t);
        end
        
        
        function ret = computeFluoBackground(this, image, segmask)
            
            fprintf('   Computing quantification image background\n');
            
            %range = this.config.intensityClassesCount;
            %if( length(this.config.intensityClassesCount(1))>1 )
            %    range = this.config.intensityClassesCount(1):this.config.intensityClassesCount(2);
            %end
            %mm = CellXMixtureModel(range);
            %mm.computeMixtureModel(image,segmask);
            
            %[~,idx]=max(mm.getMixingProportions());
            %ret = [mm.getMeanValues(idx) mm.getStdValues(idx)];
            bckVec = image(segmask);
            ret = [median(bckVec(:)) std(bckVec(:))];
            
        end
        
             
        function [ret1 ret2] = findOptimalPixelMoveAndAlignedImage(this, extimage, segmask)
            
            fprintf('   Aligning mask and image\n');
            
            % its a 2D convolution --> try if faster with conv2
            
            width = size(segmask,2);
            height = size(segmask,1);
            windsizealign = this.config.fluoAlignPixelMove;
            % take total fluorescence from the image for all combinations of image move
            totalFluoVal=zeros( (2*windsizealign+1)^2 , 1);
            pixelMoves = zeros( (2*windsizealign+1)^2 , 2);
            cnt=0;
            for xmv= -windsizealign : windsizealign
                for ymv= -windsizealign : windsizealign
                    cnt=cnt+1;
                    xmin = xmv + windsizealign + 1;
                    ymin = ymv + windsizealign + 1;
                    cropRectangle = [xmin,ymin, width-1, height-1];
                    % tempfluoImage = imcrop(extimage,cropRectangle);
                    % Own imcrop function
                    tempfluoImage = imcrop_own(extimage,cropRectangle, size(extimage,1), size(extimage,2));
                    totalFluoVal(cnt,1) =  sum(sum(tempfluoImage(segmask)));
                    pixelMoves(cnt,:) = [xmv,ymv];
                end
            end
            % output1 the pixel move
            optimalMoveIndex = totalFluoVal==max(totalFluoVal);
            ret1 = pixelMoves(optimalMoveIndex,:);
            if size(ret1,1)>1
                [~, b] =sort(sum(abs(ret1),2));
                ret1 = ret1(b(1),:);
            end
            
            % output2 the new alighned fluo image
            
            xopt = ret1(1,1) + windsizealign + 1;
            yopt = ret1(1,2) + windsizealign + 1;
            cropRectangle = [xopt,yopt, width-1, height-1];
            % ret2 = imcrop(extimage,cropRectangle);
            ret2 = imcrop_own(extimage,cropRectangle, size(extimage,1), size(extimage,2));
            
        end
        
        
        
        function measureIntensities(this,intensityCount)
            
            fprintf('   Computing intensities\n');
            
            intensityImage = this.fluoImages{intensityCount};
            smoothedIntensityImage = medfilt2(intensityImage ,this.config.wiener2params); 
            nrCells  = length(this.detectedCells);
            
            % for each cell
            for sc = 1:nrCells
                
                % --> should be transformed into 
                % CellXFluoFeatures
                fluoFeatures = CellXFluoFeatures();
                
                % store the fluotype
                fluoFeatures.fluoTypes = this.fileSet.getFluoTag(intensityCount);
                
                %----- take the total intensity distribution (features 1-4 )
                cellPixelList = this.detectedCells(sc).cellPixelListLindx;
                totalIntensityDistribution = intensityImage(cellPixelList);
                % extract the intensity characteristics for whole cell
                fluoFeatures.totalCellIntensity = sum(totalIntensityDistribution);
                quartiles = quantile(totalIntensityDistribution, [0.25, 0.5, 0.75]);
                fluoFeatures.q25TotalCellIntensity = quartiles(1);
                fluoFeatures.medianTotalCellIntensity = quartiles(2);
                fluoFeatures.q75TotalCellIntensity = quartiles(3);
                
                %----- take the membrane intensity distribution (features  5-8 )
                cellMembranePixelList = this.detectedCells(sc).membranePixelListLindx;
                totalMembraneIntensityDistribution = intensityImage(cellMembranePixelList);
                % extract the intensity characteristics for the membrane of the  cell
                fluoFeatures.totalMembraneIntensity = sum(totalMembraneIntensityDistribution);
                quartiles = quantile(totalMembraneIntensityDistribution, [0.25, 0.5, 0.75]);
                fluoFeatures.q25TotalMembraneIntensity = quartiles(1);
                fluoFeatures.medianTotalMembraneIntensity = quartiles(2);
                fluoFeatures.q75TotalMembraneIntensity= quartiles(3);
                
                
                %-----take the nuclear intensity distribution (features 9-12 )
                nuclearPixelListLindx = this.computeNuclearPixelList(intensityImage,this.detectedCells(sc));
                totalNuclearIntensityDistribution = intensityImage(nuclearPixelListLindx );
                % extract the intensity characteristics for the nucleus of the  cell
                fluoFeatures.totalNuclearIntensity = sum(totalNuclearIntensityDistribution);
                quartiles = quantile(totalNuclearIntensityDistribution, [0.25, 0.5, 0.75]);
                fluoFeatures.q25TotalNuclearIntensity = quartiles(1);
                fluoFeatures.medianTotalNuclearIntensity = quartiles(2);
                fluoFeatures.q75TotalNuclearIntensity = quartiles(3);
                
                %----take the an X% most bright pixel distribution (features 13-17 )                            
                [mostBrightAreaDistribution mostBrightAreaIndices] =...
                           this.extractBrightAreaSpatialInformation(smoothedIntensityImage,this.detectedCells(sc).cellPixelListLindx);                       
                %----compute the euler number of  the X% most bright pixel distribution (feature 13 ) 
                fluoFeatures.eulerNumberOfBrightArea = this.findEulerNumber(mostBrightAreaIndices);
                % extract the intensity characteristics for the X% bright
                % pixel area of the cell (features 14-17 )
                fluoFeatures.totalBrightAreaIntensity = sum(mostBrightAreaDistribution);
                quartiles = quantile(mostBrightAreaDistribution, [0.25, 0.5, 0.75]);
                fluoFeatures.q25TotalBrightAreaIntensity = quartiles(1);
                fluoFeatures.medianTotalBrightAreaIntensity = quartiles(2);
                fluoFeatures.q75TotalBrightAreaIntensity = quartiles(3);
               
                %---store image background values (features 18-19 )
                fluoFeatures.backgroundMeanValue = this.fluoImagesBackground{intensityCount}(1);
                fluoFeatures.backgroundStdValue = this.fluoImagesBackground{intensityCount}(2);
                
                %----- add the intensity result
               this.detectedCells(sc).addFluoFeatures(fluoFeatures);
                
            end
            
            
        end
        
        function ret = computeNuclearPixelList(this,fluoImage,currentCell)
            
            %---- compute the nuclear area (in pixels)
            nuclearVolumeSize =this.config.nuclearVolumeFraction*currentCell.cellVolume;
            % assuming that nucleus is a sphere (V = (4/3)*pi*r^3)
            nuclearRadius = (3*nuclearVolumeSize/(4*pi))^(1/3);
            nuclearAreaSizeReference = pi*nuclearRadius^2;
            
            %---crop image around the cell + make zero the
            %   pixels outside the cell
            cropRectangle = currentCell.boundingBox;
            %cellImage = imcrop(fluoImage,cropRectangle);
            %cellSegMask = imcrop(this.segmentationBinaryMask,cropRectangle);
            % Fix for speed:
            cellImage = imcrop_own(fluoImage,round(cropRectangle), size(fluoImage,1), size(fluoImage,2));
            cellSegMask = imcrop_own(this.segmentationBinaryMask,round(cropRectangle), size(fluoImage,1), size(fluoImage,2));  
            cellImageMasked = cellImage.*double(cellSegMask);
            
            %---find the nuclear center in croped image (i.e cellImageMasked )
            nuclearCenterInCropedImage = this.findNuclearCenterInCropedImage(cellImageMasked ,nuclearRadius);
            
            % take  the nuclear center in fluo image
            nuclearCenterInfluoImage = nuclearCenterInCropedImage + cropRectangle(1:2);
            
            % take the linear indices of the nucleus from the fluo
            % image
            fluoImageSize = size(fluoImage);
            ret = this.findNuclearPixelList(fluoImageSize , nuclearCenterInfluoImage,nuclearRadius);
                        
        end
        
        
        function ret = findNuclearCenterInCropedImage(this,ima,Rnuc)
            
            % grid size
            nx = 2*ceil(Rnuc)+1; x = 1:nx;
            ny = 2*ceil(Rnuc)+1; y = 1:ny;
            % make the grid
            [X,Y] = meshgrid(x,y);
            % define nuclear center
            centercoord = (nx+1)/2;
            c=[centercoord centercoord];
            
            % create nuclear circle:left side of cell in x axis
            nuccir = (X-c(1)).^2 + (Y-c(2)).^2  <= Rnuc^2 ;
            %generate nuclear mask
            nucmask =double(nuccir);
            
            % do the convolution
            tempres = conv2(ima,nucmask,'same');
            % find the center (maximum value of convolution result)
            [yc xc] = find(tempres == max(tempres(:)));
            nuclearCenter = [xc(1) yc(1)];
            
            ret = nuclearCenter;
            
        end
        
               
        function ret = findNuclearPixelList(this, fluoImageSize, nuclearCenter, nuclearRadius)
            % Ensure bounding box covers entire nuclear circle
            xStart = max(1, floor(nuclearCenter(1) - nuclearRadius));
            xEnd = min(fluoImageSize(2), ceil(nuclearCenter(1) + nuclearRadius));
            yStart = max(1, floor(nuclearCenter(2) - nuclearRadius));
            yEnd = min(fluoImageSize(1), ceil(nuclearCenter(2) + nuclearRadius));
            
            xRange = xStart:xEnd;
            yRange = yStart:yEnd;
            
            % Generate grid limited to bounding box
            [X, Y] = meshgrid(xRange, yRange);
            
            % Logical mask of pixels within nucleus circle
            mask = (X - nuclearCenter(1)).^2 + (Y - nuclearCenter(2)).^2 <= nuclearRadius^2;
            
            % Convert subscripts to linear indices in full image
            ret = int32(sub2ind(fluoImageSize, Y(mask), X(mask)));
        end
        
        
        function [ret1 ret2] = extractBrightAreaSpatialInformation(this,image,pixelList)
            
            intensityDistribution = image(pixelList);
            
            [sortedIntensityDistribution intensityPixelIndices] ...
                = sort(intensityDistribution,'descend');
            
            nrOfPixelsForBrightRegion = round(this.config.intensityBrightAreaPercentage*length(intensityDistribution));
            
            % Distribution of X% most bright area
            ret1 =sortedIntensityDistribution(1:nrOfPixelsForBrightRegion);
            
            % Linear Indices of X% most bright area
            ret2 = pixelList(intensityPixelIndices(1:nrOfPixelsForBrightRegion));
                      
        end
                 
        function ret = findEulerNumber(this, mostBrightAreaIndices)
            % Create empty logical mask (logical is faster than double zeros)
            EulerMask = false(size(this.segmentationBinaryMask));
            % Set mask pixels from indices
            EulerMask(mostBrightAreaIndices) = true;
            
            % Use bwconncomp to get connected components
            CC = bwconncomp(EulerMask);
            
            % Euler number is connected component count here
            ret = CC.NumObjects;
        end
                

    end
    
end

