## This program extracts data from a pdf diagram.

### 1. Crop the image from the pdf

A full pdf page looks like this:

![PDF Page](/src/main/resources/readmeFiles/pdf-page.pdf)

The cropped section will be like this:

![Cropped image of diagram form the pdf page](/src/main/resources/readmeFiles/croppedImage.png)

### 2. Remove the extra unnecessary text

![Cropped image with the text removed](/src/main/resources/readmeFiles/croppedImageNoText.png)

### 3. Identify the rows and columns 

At the pixel level, it iterates row by row calculating the sum of coloured pixels from every row.
The rows that contain the rectangles will have big numbers, the empty white rows will be 0.

![Image at pixel level with sum of rows](/src/main/resources/readmeFiles/Instructions%20Step%201.png)

Using the list of sums for every pixel row, the rows that contain the rectangles can be identified.
The coordinates for such rows will be saved.

![Image at pixel level, identify the rows](/src/main/resources/readmeFiles/Instructions%20Step%202.png)

The same logic applies to the columns. The coordinates for the columns will be saved.

### 4. Computes the result matrix

Using the coordinates of the rows and columns, iterate through every rectangle and count how many coloured pixels
it has. If its more then a certain threshold then the rectangle is coloured, else it is grey.

![Image at pixel level, compute rectangle colour](/src/main/resources/readmeFiles/Instructions%20Step%203.png)

Save the results in a matrix that mimics the diagram with 1 for the coloured rectangles and 0 for the non-coloured ones.

