# Evening-IDE
This is an Eclipse build that's meant to be built inside Eclipse itself. And not only that, the build is an *incremental* build!

## What's New
In addition to a build process optimized for speed and convenience, there are also other changes, mainly for Java developers.

### More Usable Refactoring
Extracting and Inlining local variables is possible from almost anywhere from a line and does what's expected.
![Animation showing freely extracting and inlining local variables](https://raw.github.com/Overruler/eclipse.jdt.core/master/refactoring_2.gif)

### A Smarter Content Assist
Suggestions list shows all implementation classes of an interface.
![Screenshot showing all List subclasses suggested in Content Assist](https://raw.github.com/Overruler/eclipse.jdt.core/master/subclasses_new.png)

Methods from the subclass are listed before methods from superclass(es).
![Screenshot showing methods from JFrame suggested before super-class methods](https://raw.github.com/Overruler/eclipse.jdt.core/master/methods_new.png)

## How to Get It, EGit way
1. Clone this repository in EGit and download all submodules.
2. After that is finished, use Import... Existing projects from Eclipse.
  * Don't import from EGit, as this doesn't track the projects correctly and can cause very long pauses during builds.
3. Import all the projects under `<repository root>/projects` without copying them into your workspace.

The first build will take a longer time. When it is done, the compiled IDE can be found in `<repository root>/projects/IDE/target/ide-1` or `<repository root>/projects/IDE/target/ide-2`.

The built IDE includes SWT and other x64 native parts for Windows, MacOS and Linux. If the MacOS and Linux versions work, *let me know!*

See also the `<repository root>/projects/IDE-all-extras/extras` folder for adding your favorite plugins, features or anything else to the final IDE.

## License
Evening-IDE is licensed under the [Eclipse Public License (EPL) v1.0](http://wiki.eclipse.org/EPL)

Some other (similar) licenses may also apply. See the source code for each submodule for details.
