# testutils
## XMLTestDataManipulator
### Problem
With the implementation of automated tests there sometimes exists a test data challenge.

In this case the challenge was that sets of xml files needed to be loaded.

There are date aspects that are important.

If we would leave it as is and reimport the same XMLs, then the dates would be useless within x days because of limitations.

### Proposed solution
Instead of playing with the server time we modify the dates in the XMLs.

Candidate solutions:
- package the object in another xml where we define placeholder definitions abstractly to update data fields
- use placeholder variables to replace on execution
- make the entire thing relative

The last option seemed quite robust in my context.

Instead of using multiple variables, the dates are all relative to the creation date.

Essentially we're doing this:
- Guess creation date
- calculate diff between now and creation date
- add that amount of time to each date

## ImageCompare
### Problem
From time to time there's a need to compare some graphics like logos and buttons or error message layouts.

This is not AI or computer vision. The pixel by pixel is suboptimal and slow but gets the job done.

The goal here is not to automatically go through an application, but rather to verify a logo or a piece of the layout.

### Proposed solution
A pixel by pixel comparison.

There is an option to:
 - Use distance or just count differences
 - Look at the entire pixel (RGB only) or split out the colors, taking into account an optional alpha channel.
 
The first implementation returns true or false. With the first inexact pixel, the function quits and returns false.

The second implementation returns a double. The result is the estimated percentage difference.

For more details, please read the code.
