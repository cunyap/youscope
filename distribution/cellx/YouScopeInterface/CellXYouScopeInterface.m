classdef CellXYouScopeInterface < handle
    %UNTITLED Summary of this class goes here
    %   Detailed explanation goes here
    
    properties
        
       fileSetNum 
       configFileName
       config
       fSet
       
       segmImage
       currentSegmentedCells
       previousSegmentedCells
       currentSegmentationMask
       previousSegmentationMask
       
       fluoTags
       flatFieldFileNames
       fluoInitialImages

       currentAssignment
       trackingIndices
       currentResult
       previousResult
       isTracking
       
       
    end
    
    methods
       
       % constructor 
       function obj = CellXYouScopeInterface(fileSetNum, configFileName,segmImage,...
                                              fluoTags,flatFieldFileNames,fluoInitialImages,...
                                              previousSegmentedCells,previousSegmentationMask,...
                                              previousResult, isTracking)
            obj.fileSetNum = fileSetNum;                              
            obj.configFileName  = configFileName;
            obj.segmImage = segmImage;
            obj.fluoTags = fluoTags;
            obj.flatFieldFileNames =flatFieldFileNames;
            obj.fluoInitialImages=fluoInitialImages;
            obj.previousSegmentedCells =  previousSegmentedCells;
            obj.previousSegmentationMask = previousSegmentationMask;
            obj.previousResult = previousResult;
            obj.isTracking = isTracking;
            
        end
        
        
        function run(this)
            %---- read the configuration file of the current experiment
            this.config = CellXConfiguration.readXML(this.configFileName);
            this.config.check();
            
            % construct a fileSet
            this.fSet = CellXFileSet(this.fileSetNum,'');
            
            % check existense of fluoFiles
            if ~isempty(this.fluoTags)
                nrFluoTags = numel(this.fluoTags);
                for nrft = 1:nrFluoTags
                    % pass the parameters of the fluo-images
                    if ~isempty(this.flatFieldFileNames)
                        % take  the flatfield files paths
                        this.fSet.addFluoImageTag('', this.fluoTags{nrft}, this.flatFieldFileNames{nrft} );
                    else
                        this.fSet.addFluoImageTag('', this.fluoTags{nrft});
                    end 
                end
            end
            
            % run the segmentation
            cellXSegmenter = CellXSegmenterYouScope(this.config, this.segmImage );
            cellXSegmenter.run();
            this.currentSegmentedCells = cellXSegmenter.getDetectedCells();
            fprintf('Detected %d cell(s) on current frame \n', numel(this.currentSegmentedCells));
            
            % run the intensity extractor
            cellXIntensityExtractor = CellXIntensityExtractorYouScope(this.config, this.fSet, this.currentSegmentedCells, this.fluoInitialImages);
            cellXIntensityExtractor.run();
            
            % take the current segmentation mask
            if ~isempty(this.config.cropRegionBoundary)
                 % APC Patch: map labeled segmentation mask back to
                 % original sized image dimensions
                dim = size(cellXSegmenter.image);
                this.currentSegmentationMask = zeros(size(this.segmImage));
                this.currentSegmentationMask(this.config.cropRegionBoundary(2) : this.config.cropRegionBoundary(2) + dim(1) - 1, ...
                    this.config.cropRegionBoundary(1) : this.config.cropRegionBoundary(1) + dim(2) - 1) =  ...
                    CellXResultExtractorYouScope.takeSegmentationMask(this.currentSegmentedCells, dim);
            else
                dim = size(this.segmImage);
                this.currentSegmentationMask = CellXResultExtractorYouScope.takeSegmentationMask(this.currentSegmentedCells, dim);
            end
            
            %  if tracking is to be done
            if ~isempty(this.previousSegmentationMask) && this.isTracking
                fprintf('Start tracking ... \n')
                trackerYouScope = CellXTrackerYouScope(this.config,...
                                             this.previousSegmentedCells,this.previousSegmentationMask,...
                                             this.currentSegmentedCells,this.currentSegmentationMask);
                trackerYouScope.run();
                this.currentAssignment = trackerYouScope.currentAssignment;               
                
                % find the new trackking Indices                
                trackingIndicesPrevious = this.previousResult.data(:,end);
                maxTrackingIndex = max(trackingIndicesPrevious);
                % for this frame
                nrCellsCurrentFrame = numel(this.currentSegmentedCells);
                this.trackingIndices =zeros(nrCellsCurrentFrame,1);
                % take the nonzero assignments
                nonZeroAssignments =  this.currentAssignment>0;
                % pass the tracking indices
                this.trackingIndices(this.currentAssignment(nonZeroAssignments)) = ...
                     trackingIndicesPrevious(nonZeroAssignments);
                % give the remaining zero values a new tracking index 
                zeroAssignments =  find(this.trackingIndices==0);
                if ~isempty(zeroAssignments)
                    newTrackingIndices = maxTrackingIndex+1:maxTrackingIndex+numel(zeroAssignments);
                    this.trackingIndices(zeroAssignments) = newTrackingIndices';
                end
                fprintf('End tracking ... \n')
            else
                this.trackingIndices = (1:numel(this.currentSegmentedCells))';
            end
            
            % store current result
            if ~isempty(this.config.cropRegionBoundary)
                 % APC Patch: map labeled segmentation cell centers back to
                 % original sized image dimensions
                 this.currentResult = CellXResultExtractorYouScope.extractSegmentationResults(...
                      this.fSet, this.currentSegmentedCells, this.config, this.trackingIndices);
                 this.currentResult.data(:,3) = this.currentResult.data(:,3) + this.config.cropRegionBoundary(1) - 1;
                 this.currentResult.data(:,4) = this.currentResult.data(:,4) + this.config.cropRegionBoundary(2) - 1;
                 % @ToDo: Note this leaves the this.currentSegmentedCells
                 % uncorrected; so all PixelListLidx map and X, Y map to
                 % the cropped segmImage.
            else
                 this.currentResult = CellXResultExtractorYouScope.extractSegmentationResults(...
                      this.fSet, this.currentSegmentedCells, this.config, this.trackingIndices);
            end
        end
        
        
        
        
    end
    
end


