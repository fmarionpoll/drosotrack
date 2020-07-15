## DrosoTrack Pages

DrosTrack is a repository for Java routines used to build plugins to run under Icy (http://icy.bioimageanalysis.org/), which is a free image analysis system derived from ImageJ and developed at Institut Pasteur.

### Features

This release includes 4 plugins associated with a dialog box and 2 tools without a dialog

```markdown
Plugins with dialog:
(1) plugins to analyze capillary feeding and to track flies into individual ROIs:
- DrosoTrack - track individual flies with an algorithm based upon a simple threshold and save results to an Excel file
- CapillaryTrack - define capillaries positions, build corresponding kymographs and track their liquid level + export to an Excel file

(2) plugins to analyze the change of leaf disk size across a stack of image files, and to define arrays of ROIs
- RoitoRoiArray - define a grid of ROIs semi-automatically
- AreaTrack - detect and measure the surface (pixels) of areas filtered and save results to an Excel file

Plugins with no dialog which are called by the dialog-based plugins:
- fmpSequence - define sequence of stacks of images which are only partly loaded into memory
- fmpTools - utility methods used by plugins with dialogs
```


### Support or Contact

Having trouble with Pages? Check out our [documentation](https://help.github.com/categories/github-pages-basics/) or [contact support](https://github.com/contact) and we will help you sort it out.
