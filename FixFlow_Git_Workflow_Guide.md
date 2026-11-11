# FixFlow -- Git & Team Workflow Guide

**Repository:** https://github.com/pinithasavidya617/fixflow-desktop\
**Default branch:** `main`

This guide explains how each team member should clone the FixFlow
repository, create an individual branch, keep it updated, commit their
assigned work, and push it to GitHub.

> **Important:** Work only on the files assigned to you in the Group
> Work Division document. Do not overwrite or modify another member's
> files without discussing it with the team.

------------------------------------------------------------------------

## 1. Before you start

Make sure:

-   Git is installed on your computer.
-   You have a GitHub account.
-   The repository owner has invited your GitHub account as a
    collaborator.
-   You have accepted the GitHub invitation.
-   You know your assigned member number, subsystem, and files.

Check that Git is installed:

``` bash
git --version
```

If Git asks you to identify yourself, configure your own name and the
email associated with your GitHub account:

``` bash
git config --global user.name "Your Name"
git config --global user.email "your-github-email@example.com"
```

Use your own GitHub identity. Do not use another member's name or email.

You can check your configuration with:

``` bash
git config --global --list
```

## 2. Clone the repository

Open a terminal, PowerShell, Git Bash, or the terminal inside your IDE.

Go to the directory where you want to keep the project. For example:

``` bash
cd Documents
```

Clone the repository:

``` bash
git clone https://github.com/pinithasavidya617/fixflow-desktop.git
```

Move into the cloned project directory:

``` bash
cd fixflow-desktop
```

The `clone` command downloads the repository and its Git history, and
configures the original GitHub repository as the remote named `origin`.

Check the remote:

``` bash
git remote -v
```

You should see the FixFlow repository URL for `origin`.

## 3. Update your local main branch

Before creating your branch, make sure your local `main` branch has the
latest changes:

``` bash
git switch main
git pull origin main
```

-   `git switch main` switches to the main branch.
-   `git pull origin main` downloads and integrates the latest changes
    from GitHub's `main` branch into your local `main`.

Always start your feature branch from the latest `main`.

## 4. Create your own branch

Each member must work on an individual branch. Do not make your module
changes directly on `main`.

Use this naming convention:

``` text
feature/member-<number>-<module>
```

Branch names for the team:

  Member     Branch
  ---------- -------------------------------
  Member 1   `feature/member-1-foundation`
  Member 2   `feature/member-2-customer`
  Member 3   `feature/member-3-repair`
  Member 4   `feature/member-4-inventory`
  Member 5   `feature/member-5-payment`
  Member 6   `feature/member-6-warranty`

Create and switch to your own branch. For example, Member 2:

``` bash
git switch -c feature/member-2-customer
```

Check your current branch:

``` bash
git branch
```

The branch marked with `*` is your current branch.

## 5. Work on your assigned files

Open the cloned `fixflow-desktop` folder in your IDE.

Implement and test your assigned subsystem according to the Group Work
Division document.

Before committing, check which files have changed:

``` bash
git status
```

Review the changes:

``` bash
git diff
```

**Important teamwork rules:**

-   Work only on your allocated files unless the team agrees otherwise.
-   Do not delete or replace files belonging to another member.
-   Do not commit generated build output such as the `target/`
    directory.
-   Do not commit local database files, IDE-specific settings,
    passwords, or other private files.
-   If your work requires changing a shared file (for example,
    `schema.sql`, `pom.xml`, or a shared UI class), discuss it with the
    team first.
-   Test your changes before committing.

## 6. Get the latest main changes before pushing

Other members may merge their work into `main` while you are working.

Before pushing your changes, update your branch with the latest `main`:

``` bash
git switch main
git pull origin main
git switch feature/member-2-customer
git merge main
```

Replace the example branch name with your own branch.

If Git reports conflicts:

1.  Do not delete files or accept changes blindly.
2.  Open each conflicted file and resolve the conflict carefully.
3.  Test the project.
4.  Stage the resolved files and complete the merge commit if Git
    requires it.

If you are unsure how to resolve a conflict, ask the team coordinator
before proceeding.

## 7. Stage and commit your changes

First, check the changed files:

``` bash
git status
```

Stage only the files you intend to commit. For example:

``` bash
git add src/main/java/com/fixflow/model/Customer.java
git add src/main/java/com/fixflow/database/CustomerDB.java
```

You can also stage a specific directory or a set of assigned files.

Avoid using `git add .` without checking `git status` first. It may
stage unrelated files.

Check what is staged:

``` bash
git status
git diff --cached
```

Commit your changes with a meaningful message:

``` bash
git commit -m "feat(customer): implement customer management"
```

Suggested commit message format:

``` text
<type>(<module>): <short description>
```

Examples:

``` bash
git commit -m "feat(customer): add customer registration"
git commit -m "feat(repair): implement repair status transitions"
git commit -m "feat(inventory): add stock deduction"
git commit -m "feat(payment): implement cash settlement"
git commit -m "test(warranty): add warranty validation tests"
```

Common types:

-   `feat` -- new functionality
-   `fix` -- bug fix
-   `test` -- add or update tests
-   `refactor` -- restructure code without changing intended behaviour
-   `docs` -- documentation changes

Each member must commit using their own configured Git identity.

## 8. Push your branch to GitHub

### First push from your branch

For the first push, use:

``` bash
git push -u origin feature/member-2-customer
```

Replace the example with your own branch name.

The `-u` option sets the upstream relationship between your local branch
and the remote branch on GitHub. This means Git remembers which remote
branch your local branch tracks.

After the first push, you can normally use:

``` bash
git push
```

Whenever you make more commits on that branch, use `git push` to upload
them.

You can check your branch and working-tree status with:

``` bash
git status
```

## 9. After pushing

Once your branch has been pushed successfully:

1.  Open the FixFlow repository on GitHub.
2.  Confirm that your branch is visible.
3.  Inform the team coordinator that your changes are pushed.
4.  Do not merge your branch into `main` unless the coordinator asks you
    to do so.

**This team workflow does not require you to create a Pull Request.**
The coordinator will handle integration into `main`.

------------------------------------------------------------------------

## 10. Daily workflow -- Quick reference

Use this sequence whenever you continue working on your module.

``` bash
# 1. Update main
git switch main
git pull origin main

# 2. Return to your own branch
git switch feature/member-2-customer

# 3. Bring the latest main changes into your branch
git merge main

# 4. Check your changes
git status

# 5. Stage your assigned files
git add <file-or-folder>

# 6. Commit
git commit -m "feat(customer): describe your change"

# 7. Push
git push
```

Replace `feature/member-2-customer` and the example commit message with
your own branch and work.

If you have not pushed your branch before, use
`git push -u origin <your-branch-name>` instead of `git push`.

## 11. Common Git commands

  -----------------------------------------------------------------------
  Command                             What it does
  ----------------------------------- -----------------------------------
  `git status`                        Shows current branch and changed
                                      files

  `git branch`                        Lists local branches

  `git switch main`                   Switches to `main`

  `git switch -c <branch>`            Creates and switches to a new
                                      branch

  `git pull origin main`              Updates local `main` from GitHub

  `git merge main`                    Merges local `main` into the
                                      current branch

  `git add <path>`                    Stages selected files

  `git diff`                          Shows unstaged changes

  `git diff --cached`                 Shows staged changes

  `git commit -m "message"`           Creates a commit

  `git push -u origin <branch>`       Pushes a branch and sets its
                                      upstream

  `git push`                          Pushes new commits to the
                                      configured upstream
  -----------------------------------------------------------------------

## 12. Important rules

1.  **Never work directly on `main`.** Use your assigned feature branch.
2.  **Use your own GitHub account and Git identity.** Commits must
    represent your own work.
3.  **Commit meaningful work.** Do not create empty or artificial
    commits just to increase the commit count.
4.  **Keep your branch updated.** Pull the latest `main` and merge it
    into your branch before pushing.
5.  **Do not overwrite another member's work.** Coordinate changes to
    shared files.
6.  **Do not push unfinished or untested changes.** Run the relevant
    tests and check the application.
7.  **Do not merge into `main` yourself.** Push your branch and notify
    the coordinator.
8.  **Never commit secrets or local database files.**

If you encounter a Git error, stop and share the complete error message
with the team coordinator before trying commands that could discard or
overwrite work.

------------------------------------------------------------------------

**Repository:** https://github.com/pinithasavidya617/fixflow-desktop\
**Default branch:** `main`\
**Integration coordinator:** Member 1
