#!/bin/bash
# Push script for wildlife-fieldops
# Usage: ./push.sh "your commit message"

REPO_URL="https://github.com/Strobingn/wildlife-fieldops.git"

echo "Setting up git credentials..."
git config user.email "austin@wildlifewhispererllc.com"
git config user.name "Austin"

echo "Adding all changes..."
git add -A

echo "Committing..."
git commit -m "${1:-Update from FieldOps app}"

echo "Pushing to GitHub..."
# Note: You'll need to enter your GitHub username and personal access token
git push origin main

echo "Done!"
