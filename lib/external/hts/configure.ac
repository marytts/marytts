# Process this file with autoconf to produce a configure script.
#  ---------------------------------------------------------------  #
# Modified by Marcela Charfuelan (DFKI) 2011 to use MARY system     #
#  ---------------------------------------------------------------  #
AC_INIT(HTS-demo_CMU-ARCTIC-SLT,2.2,hts-users@sp.nitech.ac.jp)
AC_COPYRIGHT(Copyright 2001-2011 Nagoya Institute of Technology)
AC_COPYRIGHT(Copyright 2001-2008 Tokyo Institute of Technology)



# Checks for programs.
# awk
AC_PROG_AWK



# perl
AC_PATH_PROG([PERL], [perl], [/usr/bin], [$PATH:/usr/bin])
if test ! -f "$PERL"; then
	AC_MSG_ERROR(Can't find perl)
fi



# tclsh
# tclsh search path
AC_ARG_WITH(tcl-search-path,
		AS_HELP_STRING([--with-tcl-search-path],[specify the additional tclsh (with snack) search path]),
		[tcl_search_path=$withval])

AC_PATH_PROG([TCLSH], [tclsh], [:], [${tcl_search_path}:$PATH])
if test ! -f "$TCLSH"; then
	AC_MSG_ERROR(Can't find tclsh)
fi

# check this tclsh supports snack or not
$TCLSH data/scripts/getf0.tcl > /dev/null 2>&1
if test $? = 1; then
	AC_MSG_ERROR($TCLSH doesn't support snack)
fi



# bc
AC_PATH_PROG([BC], [bc], [/usr/bin], [$PATH:/usr/bin])
if test ! -f "$BC"; then
	AC_MSG_ERROR(Can't find bc)
fi

# wc
AC_PATH_PROG([WC], [wc], [/usr/bin], [$PATH:/usr/bin])
if test ! -f "$WC"; then
	AC_MSG_ERROR(Can't find wc)
fi

# tee
AC_PATH_PROG([TEE], [tee], [/usr/bin], [$PATH:/usr/bin])
if test ! -f "$TEE"; then
	AC_MSG_ERROR(Can't find tee)
fi



# festival
# festival search path
# comented because is not used in MARY HMM voices
#AC_ARG_WITH(fest-search-path,
#		AS_HELP_STRING([--with-fest-search-path],[specify the additional festival search path]),
#		[fest_search_path=$withval])

# dumpfeats (festival)
#AC_PATH_PROG([DUMPFEATS], [dumpfeats], [:], [${fest_search_path}:$PATH])
#if test ! -f "$DUMPFEATS"; then
#	AC_MSG_ERROR(Can't find dumpfeats of festival)
#fi



# SPTK commands
# SPTK search path
AC_ARG_WITH(sptk-search-path,
		AS_HELP_STRING([--with-sptk-search-path],[specify the additional SPTK binary search path]),
		[sptk_search_path=$withval])

# x2x
AC_PATH_PROG([X2X], [x2x], [:], [${sptk_search_path}:$PATH])
if test ! -f "$X2X"; then
	AC_MSG_ERROR(Can't find x2x of SPTK)
fi

# frame
AC_PATH_PROG([FRAME], [frame], [:], [${sptk_search_path}:$PATH])
if test ! -f "$FRAME"; then
	AC_MSG_ERROR(Can't find frame of SPTK)
fi

# window
AC_PATH_PROG([WINDOW], [window], [:], [${sptk_search_path}:$PATH])
if test ! -f "$WINDOW"; then
	AC_MSG_ERROR(Can't find window of SPTK)
fi

# mgcep
if test -z "$GAMMA"; then
	AC_PATH_PROG([MGCEP], [mcep], [:], [${sptk_search_path}:$PATH])
	if test ! -f "$MGCEP"; then
		AC_MSG_ERROR(Can't find mcep of SPTK)
	fi
elif test $GAMMA -eq 0; then
	AC_PATH_PROG([MGCEP], [mcep], [:], [${sptk_search_path}:$PATH])
	if test ! -f "$MGCEP"; then
		AC_MSG_ERROR(Can't find mcep of SPTK)
	fi
else
	AC_PATH_PROG([MGCEP], [mgcep], [:], [${sptk_search_path}:$PATH])
	if test ! -f "$MGCEP"; then
		AC_MSG_ERROR(Can't find mgcep of SPTK)
	fi
fi

# lpc2lsp
AC_PATH_PROG([LPC2LSP], [lpc2lsp], [:], [${sptk_search_path}:$PATH])
if test ! -f "$LPC2LSP"; then
	AC_MSG_ERROR(Can't find lpc2lsp of SPTK)
fi

# nan
AC_PATH_PROG([NAN], [nan], [:], [${sptk_search_path}:$PATH])
if test ! -f "$NAN"; then
	AC_MSG_ERROR(Can't find nan of SPTK)
fi

# bcut
AC_PATH_PROG([BCUT], [bcut], [:], [${sptk_search_path}:$PATH])
if test ! -f "$BCUT"; then
	AC_MSG_ERROR(Can't find bcut of SPTK)
fi

# nrand
AC_PATH_PROG([NRAND], [nrand], [:], [${sptk_search_path}:$PATH])
if test ! -f "$NRAND"; then
	AC_MSG_ERROR(Can't find nrand of SPTK)
fi

# vstat
AC_PATH_PROG([VSTAT], [vstat], [:], [${sptk_search_path}:$PATH])
if test ! -f "$VSTAT"; then
	AC_MSG_ERROR(Can't find vstat of SPTK)
fi

# step
AC_PATH_PROG([STEP], [step], [:], [${sptk_search_path}:$PATH])
if test ! -f "$STEP"; then
	AC_MSG_ERROR(Can't find step of SPTK)
fi

# sopr
AC_PATH_PROG([SOPR], [sopr], [:], [${sptk_search_path}:$PATH])
if test ! -f "$SOPR"; then
	AC_MSG_ERROR(Can't find sopr of SPTK)
fi

# vopr
AC_PATH_PROG([VOPR], [vopr], [:], [${sptk_search_path}:$PATH])
if test ! -f "$VOPR"; then
	AC_MSG_ERROR(Can't find vopr of SPTK)
fi

# freqt
AC_PATH_PROG([FREQT], [freqt], [:], [${sptk_search_path}:$PATH])
if test ! -f "$FREQT"; then
	AC_MSG_ERROR(Can't find freqt of SPTK)
fi

# c2acr
AC_PATH_PROG([C2ACR], [c2acr], [:], [${sptk_search_path}:$PATH])
if test ! -f "$C2ACR"; then
	AC_MSG_ERROR(Can't find c2acr of SPTK)
fi

# mc2b
AC_PATH_PROG([MC2B], [mc2b], [:], [${sptk_search_path}:$PATH])
if test ! -f "$MC2B"; then
	AC_MSG_ERROR(Can't find mc2b of SPTK)
fi

# b2mc
AC_PATH_PROG([B2MC], [b2mc], [:], [${sptk_search_path}:$PATH])
if test ! -f "$B2MC"; then
	AC_MSG_ERROR(Can't find b2mc of SPTK)
fi

# merge
AC_PATH_PROG([MERGE], [merge], [:], [${sptk_search_path}:$PATH])
if test ! -f "$MERGE"; then
	AC_MSG_ERROR(Can't find merge of SPTK)
fi

# bcp
AC_PATH_PROG([BCP], [bcp], [:], [${sptk_search_path}:$PATH])
if test ! -f "$BCP"; then
	AC_MSG_ERROR(Can't find bcp of SPTK)
fi

# excite
AC_PATH_PROG([EXCITE], [excite], [:], [${sptk_search_path}:$PATH])
if test ! -f "$EXCITE"; then
	AC_MSG_ERROR(Can't find excite of SPTK)
fi

# lsp2lpc
AC_PATH_PROG([LSP2LPC], [lsp2lpc], [:], [${sptk_search_path}:$PATH])
if test ! -f "$LSP2LPC"; then
	AC_MSG_ERROR(Can't find lsp2lpc of SPTK)
fi

# lspcheck
AC_PATH_PROG([LSPCHECK], [lspcheck], [:], [${sptk_search_path}:$PATH])
if test ! -f "$LSPCHECK"; then
	AC_MSG_ERROR(Can't find lspcheck of SPTK)
fi

# mgc2mgc
AC_PATH_PROG([MGC2MGC], [mgc2mgc], [:], [${sptk_search_path}:$PATH])
if test ! -f "$MGC2MGC"; then
	AC_MSG_ERROR(Can't find mgc2mgc of SPTK)
fi

# mglsadf
AC_PATH_PROG([MGLSADF], [mglsadf], [:], [${sptk_search_path}:$PATH])
if test ! -f "$MGLSADF"; then
	AC_MSG_ERROR(Can't find mglsadf of SPTK)
fi

# dfs
AC_PATH_PROG([DFS], [dfs], [:], [${sptk_search_path}:$PATH])
if test ! -f "$DFS"; then
	AC_MSG_ERROR(Can't find dfs of SPTK)
fi

# minmax
AC_PATH_PROG([MINMAX], [minmax], [:], [${sptk_search_path}:$PATH])
if test ! -f "$MINMAX"; then
	AC_MSG_ERROR(Can't find minmax of SPTK)
fi

# swab
AC_PATH_PROG([SWAB], [swab], [:], [${sptk_search_path}:$PATH])
if test ! -f "$SWAB"; then
	AC_MSG_ERROR(Can't find swab of SPTK)
fi



# sox (Sound eXchange)
# sox search path
AC_ARG_WITH(sox-search-path,
		AS_HELP_STRING([--with-sox-search-path],[specify the additional sox (Sound eXchange) search path]),
		[sox_search_path=$withval])

# sox
AC_PATH_PROG([SOX], [sox], [:], [${sox_search_path}:$PATH])
if test ! -f "$SOX"; then
	AC_MSG_ERROR(Can't find sox)
fi

# sox option
SOXOPTION=2
AC_SUBST(SOXOPTION)



# HTS commands
# HTS search path
AC_ARG_WITH(hts-search-path,
		AS_HELP_STRING([--with-hts-search-path],[specify the additional HTS binary search path]),
		[hts_search_path=$withval])

# HMGenS
AC_PATH_PROG([HMGENS], [HMGenS], [:], [${hts_search_path}:$PATH])
if test ! -f "$HMGENS"; then
	AC_MSG_ERROR(Can't find HMGenS of HTS)
fi

# HCompV
AC_PATH_PROG([HCOMPV], [HCompV], [:], [${hts_search_path}:$PATH])
if test ! -f "$HCOMPV"; then
	AC_MSG_ERROR(Can't find HCompV of HTS)
fi

# HList
AC_PATH_PROG([HLIST],  [HList],  [:], [${hts_search_path}:$PATH])
if test ! -f "$HLIST"; then
	AC_MSG_ERROR(Can't find HList of HTS)
fi

# HInit
AC_PATH_PROG([HINIT],  [HInit],  [:], [${hts_search_path}:$PATH])
if test ! -f "$HINIT"; then
	AC_MSG_ERROR(Can't find HInit of HTS)
fi

# HRest
AC_PATH_PROG([HREST],  [HRest],  [:], [${hts_search_path}:$PATH])
if test ! -f "$HREST"; then
	AC_MSG_ERROR(Can't find HRest of HTS)
fi

# HERest
AC_PATH_PROG([HEREST], [HERest], [:], [${hts_search_path}:$PATH])
if test ! -f "$HEREST"; then
	AC_MSG_ERROR(Can't find HERest of HTS)
fi

# HHEd
AC_PATH_PROG([HHED],   [HHEd],   [:], [${hts_search_path}:$PATH])
if test ! -f "$HHED"; then
	AC_MSG_ERROR(Can't find HHEd of HTS)
fi

# HSMMAlign
AC_PATH_PROG([HSMMALIGN],   [HSMMAlign],   [:], [${hts_search_path}:$PATH])
if test ! -f "$HSMMALIGN"; then
	AC_MSG_ERROR(Can't find HSMMAlign of HTS)
fi



# hts_engine API
# hts_engine search path
AC_ARG_WITH(hts-engine-search-path,
		AS_HELP_STRING([--with-hts-engine-search-path],[specify the additional hts_engine binary search path]),
		[hts_engine_search_path=$withval])

# hts_engine
AC_PATH_PROG([ENGINE], [hts_engine], [:], [${hts_engine_search_path}:$PATH])
if test ! -f "$ENGINE"; then
	AC_MSG_ERROR(Can't find hts_engine)
fi



# speaker and dataset names
AC_ARG_VAR([SPEAKER],[speaker name (default=slt)])
AC_ARG_VAR([DATASET],[dataset (default=cmu_us_arctic)])

if test -z "$SPEAKER"; then
	SPEAKER=slt
fi

if test -z "$DATASET"; then
	DATASET=cmu_us_arctic
fi

AC_SUBST(SPEAKER)
AC_SUBST(DATASET)



# version number and question set name
AC_ARG_VAR([VER],[version number of this setting (default=1)])
AC_ARG_VAR([QNUM],[question set number (default='001')])

if test -z "$VER"; then
	VER=1
fi

if test -z "$QNUM"; then
	QNUM='001'
fi

AC_SUBST(VER)
AC_SUBST(QNUM)



# current directory
AC_SUBST(PWD)



# setting of mel-generalized cepstral analysis, F0 extraction, and postfiltering related settings
# parse command-line arguments
AC_ARG_VAR([FRAMELEN],[Frame length in point (default=1200)])
AC_ARG_VAR([FRAMESHIFT],[Frame shift in point (default=240)])
AC_ARG_VAR([WINDOWTYPE],[Window type -> 0: Blackman  1: Hamming  2: Hanning (default=1)])
AC_ARG_VAR([NORMALIZE],[Normalization -> 0: none  1: by power  2: by magnitude (default=1)])
AC_ARG_VAR([FFTLEN],[FFT length in point (default=2048)])
AC_ARG_VAR([FREQWARP],[Frequency warping factor (default=0.55)])
AC_ARG_VAR([GAMMA],[Pole/Zero weight factor (0: mel-cepstral analysis  1: LPC analysis  2,3,...,N: mel-generalized cepstral (MGC) analysis) (default=0)])
AC_ARG_VAR([MGCORDER],[Order of MGC analysis (default=34 for cepstral form, default=12 for LSP form)])
AC_ARG_VAR([STRORDER],[Order of strengths analysis (default=5 for 5 filter bands)])
AC_ARG_VAR([MAGORDER],[Order of Fourier magnitudes analysis (default=10 for 10 pitch harmonics)])
AC_ARG_VAR([LNGAIN],[Use logarithmic gain instead of linear gain (default=1)])
AC_ARG_VAR([LOWERF0],[Lower limit for F0 extraction in Hz (default=110)])
AC_ARG_VAR([UPPERF0],[Upper limit for F0 extraction in Hz (default=280)])
AC_ARG_VAR([NOISEMASK],[Standard deviation of white noise to mask noise in f0 extraction (default=50)])
AC_ARG_VAR([PSTFILTER],[Postfiltering factor (default=1.4)])
AC_ARG_VAR([IMPLEN],[Length of impulse response (default=4096)])
AC_ARG_VAR([SAMPFREQ],[Sampling frequency in Hz (default=48000)])
AC_ARG_VAR([STRFILTERNAME],[Name of file containig the filters for voicing strengths analysis in mixed excitation (default=filters/mix_excitation_5filters_199taps_48Kz.txt)])


# default values
if test -z "$FRAMELEN"; then
	FRAMELEN=1200
fi

if test -z "$FRAMESHIFT"; then
	FRAMESHIFT=240
fi

if test -z "$WINDOWTYPE"; then
	WINDOWTYPE=1
fi

if test -z "$NORMALIZE"; then
	NORMALIZE=1
fi

if test -z "$FFTLEN"; then
	FFTLEN=2048
fi

if test -z "$SAMPFREQ"; then
	SAMPFREQ=48000
fi

if test -z "$FREQWARP"; then
	case $SAMPFREQ in
		8000)  FREQWARP=0.31 ;;
		10000) FREQWARP=0.35 ;;
		12000) FREQWARP=0.37 ;;
		16000) FREQWARP=0.42 ;;
		22050) FREQWARP=0.45 ;;
		32000) FREQWARP=0.45 ;;
		44100) FREQWARP=0.53 ;;
		48000) FREQWARP=0.55 ;;
		*)     FREQWARP=0.00 ;;
	esac
fi

if test -z "$GAMMA"; then
	GAMMA=0
fi

if test -z "$MGCORDER"; then
	if test $GAMMA -eq 0; then
		MGCORDER=34
	else
		MGCORDER=12
	fi
fi

if test -z "$STRORDER"; then
	STRORDER=5
fi

if test -z "$MAGORDER"; then
	MAGORDER=10
fi

if test -z "$LNGAIN"; then
	LNGAIN=1
fi

if test -z "$LOWERF0"; then
	LOWERF0=110
fi 

if test -z "$UPPERF0"; then
	UPPERF0=280
fi 

if test -z "$NOISEMASK"; then
	NOISEMASK=50
fi

if test -z "$PSTFILTER"; then
	PSTFILTER=1.4
fi

if test -z "$IMPLEN"; then
	IMPLEN=4096
fi

MGCVSIZE=`expr $MGCORDER + 1`

if test $GAMMA -lt 0; then
	AC_MSG_ERROR(Cannot use MGC coefficients if GAMMA<0!)
fi

if test -z "$STRVSIZE"; then
	STRVSIZE=$STRORDER
fi

if test -z "$MAGVSIZE"; then
	MAGVSIZE=$MAGORDER
fi

if test -z "$STRFILTERNAME"; then
	STRFILTERNAME=filters/mix_excitation_5filters_199taps_48Kz.txt
fi

# substitute variables
AC_SUBST(FRAMELEN)
AC_SUBST(FRAMESHIFT)
AC_SUBST(WINDOWTYPE)
AC_SUBST(NORMALIZE)
AC_SUBST(FFTLEN)
AC_SUBST(FREQWARP)
AC_SUBST(GAMMA)
AC_SUBST(MGCORDER)
AC_SUBST(STRORDER)
AC_SUBST(MAGORDER)
AC_SUBST(LOWERF0)
AC_SUBST(UPPERF0)
AC_SUBST(NOISEMASK)
AC_SUBST(PSTFILTER)
AC_SUBST(IMPLEN)
AC_SUBST(SAMPFREQ)
AC_SUBST(MGCVSIZE)
AC_SUBST(STRVSIZE)
AC_SUBST(MAGVSIZE)
AC_SUBST(STRFILTERNAME)


# number of windows
# parse command-line arguments
AC_ARG_VAR([NMGCWIN],[number of delta windows for MGC coefficients (default=3)])
AC_ARG_VAR([NSTRWIN],[number of delta windows for STR coefficients (default=3)])
AC_ARG_VAR([NMAGWIN],[number of delta windows for MAG coefficients (default=3)])
AC_ARG_VAR([NLF0WIN],[number of delta windows for log F0 values (default=3)])

# default values
if test -z "$NMGCWIN"; then
	NMGCWIN=3
fi

if test -z "$NSTRWIN"; then
	NSTRWIN=3
fi

if test -z "$NMAGWIN"; then
	NMAGWIN=3
fi

if test -z "$NLF0WIN"; then
	NLF0WIN=3
fi

F0STREN=`expr $NLF0WIN + 1`

# substitute variables
AC_SUBST(NMGCWIN)
AC_SUBST(NSTRWIN)
AC_SUBST(NMAGWIN)
AC_SUBST(NLF0WIN)
AC_SUBST(F0STREN)



# model training settings
# parse command-line arguments
AC_ARG_VAR([NSTATE],[number of HMM states (default=5)])
AC_ARG_VAR([NITER],[number of iterations of embedded training (default=5)])
AC_ARG_VAR([WFLOOR],[mixture weight flooring scale (default=5000)])
AC_ARG_VAR([DAEM],[DAEM algorithm for parameter estimation (0:off or 1:on, default=0)])
AC_ARG_VAR([DAEMNITER],[number of iterations of DAEM-based embedded training (default=10)])
AC_ARG_VAR([DAEMALPHA],[schedule of updating temperature parameter for DAEM (default=1.0)])
AC_ARG_VAR([MAXDEV],[maximum duration scaling factor (default=10)])
AC_ARG_VAR([MINDUR],[minimum duration to be evaluated (default=5)])
AC_ARG_VAR([NMGCTRANSBLK],[number of blocks for MGC transforms (default=3)])
AC_ARG_VAR([NSTRTRANSBLK],[number of blocks for STR transforms (default=3)])
AC_ARG_VAR([NMAGTRANSBLK],[number of blocks for MAG transforms (default=3)])
AC_ARG_VAR([NLF0TRANSBLK],[number of blocks for log F0 transforms (default=1)])
AC_ARG_VAR([MGCBANDWIDTH],[band width for MGC transforms (default=35 for cepstral form, derault=2 for LSP form)])
AC_ARG_VAR([STRBANDWIDTH],[band width for STR transforms (default=5)])
AC_ARG_VAR([MAGBANDWIDTH],[band width for MAG transforms (default=10)])
AC_ARG_VAR([LF0BANDWIDTH],[band width for log F0 transforms (default=1)])

# default values
if test -z "$NSTATE"; then
	NSTATE=5
fi

if test -z "$NITER"; then
	NITER=5
fi

if test -z "$WFLOOR"; then
	WFLOOR=5000
fi

if test -z "$DAEM"; then
	DAEM=0
fi

if test -z "$DAEMNITER"; then
	DAEMNITER=10
fi

if test -z "$DAEMALPHA"; then
	DAEMALPHA=1.0
fi

if test -z "$MAXDEV"; then
	MAXDEV=10
fi

if test -z "$MINDUR"; then
	MINDUR=5
fi

if test -z "$NMGCTRANSBLK"; then
	NMGCTRANSBLK=3
fi

if test -z "$NSTRTRANSBLK"; then
	NSTRTRANSBLK=3
fi

if test -z "$NMAGTRANSBLK"; then
	NMAGTRANSBLK=3
fi

if test -z "$NLF0TRANSBLK"; then
	NLF0TRANSBLK=1
fi

if test -z "$MGCBANDWIDTH"; then
	if test $GAMMA -eq 0; then
		MGCBANDWIDTH=$MGCVSIZE
	else
		MGCBANDWIDTH=2
	fi
fi

if test -z "$STRBANDWIDTH"; then
	STRBANDWIDTH=$STRORDER
fi

if test -z "$MAGBANDWIDTH"; then
	MAGBANDWIDTH=$MAGORDER
fi

if test -z "$LF0BANDWIDTH"; then
	LF0BANDWIDTH=1
fi

# substitute variables
AC_SUBST(NSTATE)
AC_SUBST(NITER)
AC_SUBST(WFLOOR)
AC_SUBST(DAEM)
AC_SUBST(DAEMNITER)
AC_SUBST(DAEMALPHA)
AC_SUBST(MAXDEV)
AC_SUBST(MINDUR)
AC_SUBST(NMGCTRANSBLK)
AC_SUBST(NSTRTRANSBLK)
AC_SUBST(NMAGTRANSBLK)
AC_SUBST(NLF0TRANSBLK)
AC_SUBST(MGCBANDWIDTH)
AC_SUBST(STRBANDWIDTH)
AC_SUBST(MAGBANDWIDTH)
AC_SUBST(LF0BANDWIDTH)


# parameter generation algorithm settings
# parse command-line arguments
AC_ARG_VAR([MAXEMITER],[max EM iteration (default=20)])
AC_ARG_VAR([EMEPSILON],[convergence factor for EM iteration (default=0.0001)])
AC_ARG_VAR([USEGV],[turn on GV-based parameter generation algorithm (0:off or 1:on, default=1)])
AC_ARG_VAR([MAXGVITER],[maximum number of iterations of GV-based parameter generation algorithm (default=50)])
AC_ARG_VAR([GVEPSILON],[convergence factor for GV iteration (default=0.0001)])
AC_ARG_VAR([MINEUCNORM],[minimum Euclid norm for GV iteration (default=0.01)])
AC_ARG_VAR([STEPINIT],[initial step size (default=1.0)])
AC_ARG_VAR([STEPINC],[step size acceleration factor (default=1.2)])
AC_ARG_VAR([STEPDEC],[step size deceleration factor (default=0.5)])
AC_ARG_VAR([HMMWEIGHT],[weight for HMM output prob. (default=1.0)])
AC_ARG_VAR([GVWEIGHT],[weight for GV output prob. (default=1.0)])
AC_ARG_VAR([OPTKIND],[optimization method (STEEPEST, NEWTON, or LBFGS) (default=NEWTON)])
AC_ARG_VAR([NOSILGV],[turn on GV without silent and pause phoneme (0:off or 1:on, default=1)])
AC_ARG_VAR([CDGV],[turn on context-dependent GV (0:off or 1:on, default=1)])

# default values
if test -z "$MAXEMITER"; then
	MAXEMITER=20
fi

if test -z "$EMEPSILON"; then
	EMEPSILON=0.0001
fi

if test -z "$USEGV"; then
	USEGV=1
fi

if test -z "$MAXGVITER"; then
	MAXGVITER=50
fi

if test -z "$GVEPSILON"; then
	GVEPSILON=0.0001
fi

if test -z "$MINEUCNORM"; then
	MINEUCNORM=0.01
fi

if test -z "$STEPINIT"; then
	STEPINIT=1.0
fi

if test -z "$STEPINC"; then
	STEPINC=1.2
fi

if test -z "$STEPDEC"; then
	STEPDEC=0.5
fi

if test -z "$HMMWEIGHT"; then
	HMMWEIGHT=1.0
fi

if test -z "$GVWEIGHT"; then
	GVWEIGHT=1.0
fi

if test -z "$OPTKIND"; then
	OPTKIND=NEWTON
fi

if test -z "$NOSILGV"; then
	NOSILGV=1
fi

if test -z "$CDGV"; then
	CDGV=1
fi

# substitute variables
AC_SUBST(MAXEMITER)
AC_SUBST(EMEPSILON)
AC_SUBST(USEGV)
AC_SUBST(MAXGVITER)
AC_SUBST(GVEPSILON)
AC_SUBST(MINEUCNORM)
AC_SUBST(STEPINIT)
AC_SUBST(STEPINC)
AC_SUBST(STEPDEC)
AC_SUBST(HMMWEIGHT)
AC_SUBST(GVWEIGHT)
AC_SUBST(OPTKIND)
AC_SUBST(NOSILGV)
AC_SUBST(CDGV)



# output data/Makefile and scripts/Config.pm
AC_OUTPUT(data/Makefile scripts/Config.pm Makefile)
