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
