Recommended Workflow:
1. Cloning the Repository: Clone the repository by running:
git clone https://github.com/tzy0913/Biolock.git

2. Creating a New Branch: Create a new branch for the features or fix you are working on:
git checkout -b "feature-branch-name"
This avoids everyone working directly on the main branch, which helps prevent conflicts and ensures that the main branch remains stable.

3. Making Changes and Committing: After making changes, commit your work to your own branch:
git add .
git commit -m "Description of changes"

4. Pushing to your Branch: Once committed, push your branch to the remote repository:
git push origin "feature-branch-name"

5. Pull the latest changes from the main branch before you start working on your feature branches:
git pull origin main

After pulling from the main branch, you can proceed to work on your code and later push it to your own branch. Please notify in group everytime u push something so that i can merge it into main branch. Thanks!
