#  Main targets can be executed directly, and they are:
#  
#     build                    build a specific configuration
#     clean                    remove built files from a configuration
#     clobber                  remove all built files
#     all                      build all configurations
#     help                     print help message
#  
#  Targets .build-impl, .clean-impl, .clobber-impl, .all-impl, and
#  .help-impl are implemented in nbproject/makefile-impl.mk.
#
# NOCDDL



# Environment 
MKDIR=mkdir
CP=cp
CCADMIN=CCadmin
RANLIB=ranlib



# build
build: .build-post-$(CONF)

.build-pre: .build-pre-$(CONF)
# Add your pre 'build' code here...

.build-pre-visualvm.exe: visualvm.rc
	windres -ovisualvm.res -Ocoff visualvm.rc

.build-pre-visualvm64.exe: visualvm.rc
	x86_64-w64-mingw32-windres.exe -ovisualvm64.res -Ocoff visualvm.rc

.build-post-visualvm.exe: .build-impl
	cp visualvm.exe ../visualvm.exe
	strip ../visualvm.exe
	
.build-post-visualvm64.exe: .build-impl
	cp visualvm64.exe ../visualvm64.exe
	strip ../visualvm.exe

											

# clean
clean: .clean-post

.clean-pre:
# Add your pre 'clean' code here...

.clean-post: .clean-impl
# Add your post 'clean' code here...



# clobber
clobber: .clobber-post

.clobber-pre:
# Add your pre 'clobber' code here...

.clobber-post: .clobber-impl
# Add your post 'clobber' code here...



# all
all: .all-post

.all-pre:
# Add your pre 'all' code here...

.all-post: .all-impl
# Add your post 'all' code here...



# help
help: .help-post

.help-pre:
# Add your pre 'help' code here...

.help-post: .help-impl
# Add your post 'help' code here...



# include project implementation makefile
include nbproject/Makefile-impl.mk
