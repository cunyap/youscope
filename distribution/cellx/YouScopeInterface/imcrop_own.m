function [ ImgCrop ] = imcrop_own( RawImg, ROI, MaxHeight, MaxWidth )
%imcrop_own Fast implementation to crop an Image with a ROI. Currenly only
%for 1D images.
%
% Input:
%  RawImage         : [NxM, double] Image to be cropped
%  ROI              : [1x4, double] Array with x,y coordinates and side
%                                   lengths of rectangle to crop s1, s2.
%  MaxHeight        : [1x1, double] Max height of image in px.
%  MaxWidth         : [1x1, double] Max width of image in px.
% 
% Output:
%  ImgCrop          : [nxm, double] Croped image with size of specified
%                                   side lengts s1 and s2 in ROI

% assert(isa(RawImg, 'double') || isa(RawImg, 'locical'))
% assert ( all(size(RawImg)>=[1, 1]));
% assert(isa(ROI, 'double'))
% assert ( all ( size (ROI) == [1, 4] ) )
% assert(isa(MaxHeight, 'double'))
% assert(isa(MaxWidth, 'double'))

xmin = ROI(1,1);
ymin = ROI(1,2);
xmax = ROI(1,1)+(ROI(1,3));
ymax = ROI(1,2)+(ROI(1,4));

if ymin < 1
  ymin = 1;
end
if xmin < 1
  xmin = 1;
end
if ymax < 1
  ymax = 1;
end
if xmax < 1
  xmax = 1;
end

if ymin > MaxHeight
  ymin = MaxHeight;
end
if ymax > MaxHeight
  ymax = MaxHeight;
end
if xmin > MaxWidth
  xmin = MaxWidth;
end
if xmax > MaxWidth
  xmax = MaxWidth;
end

% Slightly slower than as above
% xmin = max(1, min(ROI(1, 1), MaxHeight));
% xmax = max(1, min(ROI(1,1)+(ROI(1,3)), MaxHeight));
% ymin = max(1, min(ROI(1, 2), MaxWidth));
% ymax = max(1, min(ROI(1,2)+(ROI(1,4)), MaxWidth));

ImgCrop = RawImg(ymin:ymax,xmin:xmax,:);

end

