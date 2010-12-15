#!/usr/bin/perl
# ----------------------------------------------------------------- #
#           The HMM-Based Speech Synthesis System (HTS)             #
#           developed by HTS Working Group                          #
#           http://hts.sp.nitech.ac.jp/                             #
# ----------------------------------------------------------------- #
#                                                                   #
#  Copyright (c) 2001-2008  Nagoya Institute of Technology          #
#                           Department of Computer Science          #
#                                                                   #
#                2001-2008  Tokyo Institute of Technology           #
#                           Interdisciplinary Graduate School of    #
#                           Science and Engineering                 #
#                                                                   #
# All rights reserved.                                              #
#                                                                   #
# Redistribution and use in source and binary forms, with or        #
# without modification, are permitted provided that the following   #
# conditions are met:                                               #
#                                                                   #
# - Redistributions of source code must retain the above copyright  #
#   notice, this list of conditions and the following disclaimer.   #
# - Redistributions in binary form must reproduce the above         #
#   copyright notice, this list of conditions and the following     #
#   disclaimer in the documentation and/or other materials provided #
#   with the distribution.                                          #
# - Neither the name of the HTS working group nor the names of its  #
#   contributors may be used to endorse or promote products derived #
#   from this software without specific prior written permission.   #
#                                                                   #
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND            #
# CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,       #
# INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF          #
# MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE          #
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS #
# BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,          #
# EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED   #
# TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,     #
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON #
# ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,   #
# OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY    #
# OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE           #
# POSSIBILITY OF SUCH DAMAGE.                                       #
# ----------------------------------------------------------------- #

$|=1;

if (@ARGV<1) {
   print "usage: Training.pl Config.pm\n";
   exit(0);
}

# load configuration variables
require($ARGV[0]);


# File locations =========================
# data directory
$datdir = "$prjdir/data";

# data location file 
$scp{'trn'} = "$datdir/scp/train.scp";
$scp{'gen'} = "$datdir/scp/gen.scp";

# model list files
$lst{'mon'} = "$datdir/lists/mono.list";
$lst{'ful'} = "$datdir/lists/full.list";
$lst{'all'} = "$datdir/lists/full_all.list";
 
# master label files
$mlf{'mon'} = "$datdir/labels/mono.mlf";
$mlf{'ful'} = "$datdir/labels/full.mlf";

# configuration variable files
$cfg{'trn'} = "$prjdir/configs/trn.cnf";
$cfg{'nvf'} = "$prjdir/configs/nvf.cnf";
$cfg{'cnv'} = "$prjdir/configs/cnv.cnf";
$cfg{'stc'} = "$prjdir/configs/stc.cnf";
$cfg{'syn'} = "$prjdir/configs/syn.cnf";
$cfg{'gv'}  = "$prjdir/configs/gv.cnf";

# model topology definition file
$prtfile = "$prjdir/proto/ver$ver/";

# model files
foreach $set (@SET){
   $model{$set}   = "$prjdir/models/qst${qnum}/ver${ver}/${set}";
   $hinit{$set}   = "$model{$set}/HInit";
   $hrest{$set}   = "$model{$set}/HRest";
   $vfloors{$set} = "$model{$set}/vFloors";
   $initmmf{$set} = "$model{$set}/init.mmf";
   $monommf{$set} = "$model{$set}/monophone.mmf";
   $fullmmf{$set} = "$model{$set}/fullcontext.mmf";
   $clusmmf{$set} = "$model{$set}/clustered.mmf";
   $untymmf{$set} = "$model{$set}/untied.mmf";
   $reclmmf{$set} = "$model{$set}/re_clustered.mmf";
   $rclammf{$set} = "$model{$set}/re_clustered_all.mmf";
   $tiedlst{$set} = "$model{$set}/tiedlist";
   $gvmmf{$set}   = "$model{$set}/gv.mmf";
   $gvlst{$set}   = "$model{$set}/gv.list";
   $stcmmf{$set}  = "$model{$set}/stc.mmf";
   $stcammf{$set} = "$model{$set}/stc_all.mmf";
   $stcbase{$set} = "$model{$set}/stc.base";
}

# statistics files
foreach $set (@SET){
   $stats{$set} = "$prjdir/stats/qst${qnum}/ver${ver}/${set}.stats";
}

# model edit files
foreach $set (@SET){
   $hed{$set} = "$prjdir/edfiles/qst${qnum}/ver${ver}/${set}";
   $lvf{$set} = "$hed{$set}/lvf.hed";
   $m2f{$set} = "$hed{$set}/m2f.hed";
   $mku{$set} = "$hed{$set}/mku.hed";
   $unt{$set} = "$hed{$set}/unt.hed";
   $upm{$set} = "$hed{$set}/upm.hed";
   foreach $type (@{$ref{$set}}) {
      $cnv{$type} = "$hed{$set}/cnv_$type.hed";
      $cxc{$type} = "$hed{$set}/cxc_$type.hed";
   }
}

# questions about contexts
foreach $set (@SET){
   foreach $type (@{$ref{$set}}) {
      $qs{$type} = "$datdir/questions/questions_qst${qnum}.hed";
   }
}

# decision tree files
foreach $set (@SET){
   $trd{$set} = "${prjdir}/trees/qst${qnum}/ver${ver}/${set}";
   foreach $type (@{$ref{$set}}) {
      $mdl{$type} = "-m -a $mdlf{$type}" if($thr{$type} eq '000');
      $tre{$type} = "$trd{$set}/${type}.inf";
   }
}

# converted model & tree files for hts_engine
$voice = "$prjdir/voices/qst${qnum}/ver${ver}";
foreach $set (@SET) {
   foreach $type (@{$ref{$set}}) {
      $trv{$type} = "$voice/tree-${type}.inf";
      $pdf{$type} = "$voice/${type}.pdf";
   }
}

# window files for parameter generation
$windir = "${datdir}/win";
foreach $type (@cmp) {
   for ($d=1;$d<=$nwin{$type};$d++) {
      $win{$type}[$d-1] = "${type}.win${d}";
   }
}

# global variance pdf files for parameter generation
$gvdir = "${datdir}/gv";
foreach $type (@cmp) {
   $gvpdf{$type} = "$gvdir/gv-${type}.pdf";
}

# model structure
$vSize{'total'} = 0;
$nstream{'total'} = 0;
$nPdfStreams = 0;
foreach $type (@cmp) {
   $vSize{$type}      = $nwin{$type}*$ordr{$type};
   $vSize{'total'}   += $vSize{$type};
   $nstream{$type}    = $stre{$type}-$strb{$type}+1;
   $nstream{'total'} += $nstream{$type};
   $nPdfStreams++;
}

# HTS Commands & Options ========================
$HCompV         = "$HCOMPV -A    -C $cfg{'trn'} -D -T 1 -S $scp{'trn'}";
$HInit          = "$HINIT  -A    -C $cfg{'trn'} -D -T 1 -S $scp{'trn'}                -m 1 -u tmvw    -w $wf";
$HRest          = "$HREST  -A    -C $cfg{'trn'} -D -T 1 -S $scp{'trn'}                -m 1 -u tmvw    -w $wf";
$HERest{'mon'}  = "$HEREST -A    -C $cfg{'trn'} -D -T 1 -S $scp{'trn'} -I $mlf{'mon'} -m 1 -u tmvwdmv -w $wf -t $beam ";
$HERest{'ful'}  = "$HEREST -A -B -C $cfg{'trn'} -D -T 1 -S $scp{'trn'} -I $mlf{'ful'} -m 1 -u tmvwdmv -w $wf -t $beam ";
$HHEd{'trn'}    = "$HHED   -A -B -C $cfg{'trn'} -D -T 1 -p -i";
$HHEd{'cnv'}    = "$HHED   -A -B -C $cfg{'cnv'} -D -T 1 -p -i";
$HMGenS         = "$HMGENS -A -B -C $cfg{'syn'} -D -T 1 -S $scp{'gen'} -t $beam ";

# Initial Values ========================
$minocc = 0.0;

# =============================================================
# ===================== Main Program ==========================
# =============================================================

# preparing environments
if ($MKEMV) {
   print_time("preparing environments");
   
   # make directories
   foreach $dir ('models', 'stats', 'edfiles', 'trees', 'voices', 'gen') {
      mkdir "$prjdir/$dir", 0755;
      mkdir "$prjdir/$dir/qst${qnum}", 0755;
      mkdir "$prjdir/$dir/qst${qnum}/ver${ver}", 0755;
   }
   foreach $set (@SET) {
      mkdir "$model{$set}", 0755;
      mkdir "$hinit{$set}", 0755;
      mkdir "$hrest{$set}", 0755;
      mkdir "$hed{$set}", 0755;
      mkdir "$trd{$set}", 0755;
   }

   # make config files 
   mkdir "$prjdir/configs",0755;
   make_config();

   # make model prototype definition file;
   mkdir "$prjdir/proto",0755;
   mkdir "$prjdir/proto/ver$ver",0755;
   make_proto();

   # convert GV pdf -> GV mmf
   if ($useGV) {
      conv_gvpdf2mmf();
   }
}


# HCompV (computing variance floors)
if ($HCMPV) {
   print_time("computing variance floors");
   
   # compute variance floors 
   shell("$HCompV -M $model{cmp} -o $initmmf{'cmp'} $prtfile");
   shell("head -n 1 $prtfile > $initmmf{'cmp'}");
   shell("cat $vfloors{cmp} >> $initmmf{'cmp'}");
}


# HInit & HRest (initialization & reestimation)
if ($IN_RE) {
   print_time("initialization & reestimation");

   open(LIST, $lst{'mon'}) || die "Cannot open $!";
   while ($phone = <LIST>) {
      # trimming leading and following whitespace characters
      $phone =~ s/^\s+//;
      $phone =~ s/\s+$//;

      # skip a blank line
      if ($phone eq '') {
         next;
      }
      $lab = $mlf{'mon'};

      print "=============== $phone ================\n";
      if (grep($_ eq $phone, keys %mdcp) > 0){
         print "use $mdcp{$phone} instead of $phone\n";
         $set = 'cmp';
         open(SRC, "$hrest{$set}/$mdcp{$phone}") || die "Cannot open $!";
         open(TGT, ">$hrest{$set}/$phone") || die "Cannot open $!";
         while (<SRC>){
            s/~h \"$mdcp{$phone}\"/~h \"$phone\"/;
            print TGT;
         }
         close(TGT);
         close(SRC);
      } 
      else {
         shell("$HInit -H $initmmf{'cmp'} -M $hinit{'cmp'} -I $lab -l $phone -o $phone $prtfile");
         shell("$HRest -H $initmmf{'cmp'} -M $hrest{'cmp'} -I $lab -l $phone -g $hrest{'dur'}/$phone $hinit{'cmp'}/$phone");
      }
   }
   close(LIST);
}


# HHEd (making a monophone mmf) 
if ($MMMMF) {
   print_time("making a monophone mmf");

   foreach $set (@SET) {
      open (EDFILE,">$lvf{$set}") || die "Cannot open $!";
      
      # load variance floor macro
      print EDFILE "// load variance flooring macro\n";
      print EDFILE "FV \"$vfloors{$set}\"\n"; 
      
      # tie stream weight macro
      foreach $type (@{$ref{$set}}) {
         if ($strw{$type}!=1.0) {
            print  EDFILE "// tie stream weights\n";
            printf EDFILE "TI SW_all {*.state[%d-%d].weights}\n", 2, $nState-1;
            last;
         }
      }

      close(EDFILE);

      shell("$HHEd{'trn'} -d $hrest{$set} -w $monommf{$set} $lvf{$set} $lst{'mon'}");
      shell("gzip -c $monommf{$set} > $monommf{$set}.nonembedded.gz");
   }
}


# HERest (embedded reestimation (monophone))
if ($ERST0) {
   print_time("embedded reestimation (monophone)");
   
   for ($i=1;$i<=$nIte;$i++) {
      # embedded reestimation
      print("\n\nIteration $i of Embedded Re-estimation\n");
      shell("$HERest{'mon'} -H $monommf{'cmp'} -N $monommf{'dur'} -M $model{'cmp'} -R $model{'dur'} $lst{'mon'} $lst{'mon'}");
   }

   # compress reestimated model
   foreach $set (@SET) {
      shell("gzip -c $monommf{$set} > ${monommf{$set}}.embedded.gz");
   }
}


# HHEd (copying monophone mmf to fullcontext one) 
if ($MN2FL) {
   print_time("copying monophone mmf to fullcontext one");

   foreach $set (@SET) {
      open (EDFILE, ">$m2f{$set}") || die "Cannot open $!";
      open (LIST,   "$lst{'mon'}") || die "Cannot open $!";

      print EDFILE "// copy monophone models to fullcontext ones\n";
      print EDFILE "CL \"$lst{'ful'}\"\n\n";    # CLone monophone to fullcontext

      print EDFILE "// tie state transition probability\n";
      while ($phone = <LIST>) {
         # trimming leading and following whitespace characters
         $phone =~ s/^\s+//;
         $phone =~ s/\s+$//;

         # skip a blank line
         if ($phone eq '') {
            next;
         }
         print EDFILE "TI T_${phone} {*-${phone}+*.transP}\n"; # TIe transition prob
      }
      close(LIST);
      close(EDFILE);

      shell("$HHEd{'trn'} -H $monommf{$set} -w $fullmmf{$set} $m2f{$set} $lst{'mon'}");
      shell("gzip -c $fullmmf{$set} > $fullmmf{$set}.nonembedded.gz");
   }
}


# HERest (embedded reestimation (fullcontext))
if ($ERST1) {
   print_time("embedded reestimation (fullcontext)");

   $opt = "-C $cfg{'nvf'} -s $stats{'cmp'} -w 0.0";

   # embedded reestimation   
   print("\n\nEmbedded Re-estimation\n");
   shell("$HERest{'ful'} -H $fullmmf{'cmp'} -N $fullmmf{'dur'} -M $model{'cmp'} -R $model{'dur'} $opt $lst{'ful'} $lst{'ful'}");
   
   # compress reestimated model
   foreach $set (@SET) {
      shell("gzip -c $fullmmf{$set} > ${fullmmf{$set}}.embedded.gz");
   }
}

# convert cmp stats to duration ones
convstats();

# HHEd (tree-based context clustering)
if ($CXCL1) {
   print_time("tree-based context clustering");

   foreach $set (@SET) {
      shell("cp $fullmmf{$set} $clusmmf{$set}");

      # tree-based clustering
      $footer = "";
      foreach $type (@{$ref{$set}}) {
         if ($strw{$type}>0.0) {
            $minocc = $mocc{$type};
            make_config();
            make_edfile_state($type);
            shell("$HHEd{'trn'} -H $clusmmf{$set} $mdl{$type} -w $clusmmf{$set} $cxc{$type} $lst{'ful'}");
            $footer .= "_$type";
            shell("gzip -c $clusmmf{$set} > $clusmmf{$set}$footer.gz");
         }
      }
   }
}


# HERest (embedded reestimation (clustered))
if ($ERST2) {
   print_time("embedded reestimation (clustered)");
     
   for ($i=1;$i<$nIte;$i++) {
      print("\n\nIteration $i of Embedded Re-estimation\n");
      shell("$HERest{'ful'} -H $clusmmf{'cmp'} -N $clusmmf{'dur'} -M $model{'cmp'} -R $model{'dur'} $lst{'ful'} $lst{'ful'}");
   }

   # compress reestimated mmfs
   foreach $set (@SET) {
      shell("gzip -c $clusmmf{$set} > $clusmmf{$set}.embedded2.gz");
   }
}


# HHEd (untying the parameter sharing structure)
if ($UNTIE) {
   print_time("untying the parameter sharing structure");

   foreach $set (@SET) {
      make_edfile_untie($set);
      shell("$HHEd{'trn'} -H $clusmmf{$set} -w $untymmf{$set} $unt{$set} $lst{'ful'}");
   }
}


# fix variables
foreach $set (@SET) { 
   $stats{$set} .= ".untied";
   foreach $type (@{$ref{$set}}) {
      $tre{$type}   .= ".untied";
      $cxc{$type}   .= ".untied";
   }
}


# HERest (embedded reestimation (untied))
if ($ERST3) {
   print_time("embedded reestimation (untied)");

   $opt = "-C $cfg{'nvf'} -s $stats{'cmp'} -w 0.0";

   print("\n\nEmbedded Re-estimation for untied mmfs\n");
   shell("$HERest{'ful'} -H $untymmf{'cmp'} -N $untymmf{'dur'} -M $model{'cmp'} -R $model{'dur'} $opt $lst{'ful'} $lst{'ful'}");
}

# convert cmp stats to duration ones
convstats();

# HHEd (tree-based context clustering)
if ($CXCL2) {
   print_time("tree-based context clustering");

   # tree-based clustering
   foreach $set (@SET) {
      shell("cp $untymmf{$set} $reclmmf{$set}");

      $footer = "";
      foreach $type (@{$ref{$set}}) {
         $minocc = $mocc{$type};
         make_config();
         make_edfile_state($type);
         shell("$HHEd{'trn'} -H $reclmmf{$set} $mdl{$type} -w $reclmmf{$set} $cxc{$type} $lst{'ful'}");

         $footer .= "_$type";
         shell("gzip -c $reclmmf{$set} > $reclmmf{$set}$footer.gz");
      }
      shell("gzip -c $reclmmf{$set} > $reclmmf{$set}.noembedded.gz");
   }
}


# HERest (embedded reestimation (re-clustered)) 
if ($ERST4) {
   print_time("embedded reestimation (re-clustered)");

   for ($i=1;$i<=$nIte;$i++) {
      print("\n\nIteration $i of Embedded Re-estimation\n");
      shell("$HERest{'ful'} -H $reclmmf{'cmp'} -N $reclmmf{'dur'} -M $model{'cmp'} -R $model{'dur'} $lst{'ful'} $lst{'ful'}");
   }

   # compress reestimated mmfs
   foreach $set (@SET) {
      shell("gzip -c $reclmmf{$set} > $reclmmf{$set}.embedded.gz");
   }
}


# HHEd (making unseen models (1mix))
if ($MKUN1) {
   print_time("making unseen models (1mix)");
   
   foreach $set (@SET) {
      make_edfile_mkunseen($set);
      shell("$HHEd{'trn'} -H $reclmmf{$set} -w $rclammf{$set}.1mix $mku{$set} $lst{'ful'}");
   }
}


# HMGenS (generating speech parameter sequences (1mix))
if ($PGEN1) {
   print_time("generating speech parameter sequences (1mix)");

   $mix = '1mix';
   
   mkdir "${prjdir}/gen/qst${qnum}/ver${ver}/$mix", 0755;
   for ($pgtype=0; $pgtype<=2; $pgtype++) {
      # prepare output directory 
      $dir = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype";
      mkdir $dir, 0755; 
            
      # generate parameter
      shell("$HMGenS -c $pgtype -H $rclammf{'cmp'}.$mix -N $rclammf{'dur'}.$mix -M $dir $tiedlst{'cmp'} $tiedlst{'dur'}");
   }
}


# SPTK (synthesizing waveforms (1mix))
if ($WGEN1) {
   print_time("synthesizing waveforms (1mix)");

   $mix = '1mix';

   mkdir "${prjdir}/gen/qst${qnum}/ver${ver}/$mix", 0755;
   for ($pgtype=0; $pgtype<=2; $pgtype++) { 
      gen_wave("${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype");
   }
}


# HHEd (converting mmfs to the hts_engine file format)
if ($CONVM) {
   print_time("converting mmfs to the hts_engine file format");

   # models and trees
   foreach $set (@SET) {
      foreach $type (@{$ref{$set}}) {
         make_edfile_convert($type);
         shell("$HHEd{'cnv'} -H $reclmmf{$set} $cnv{$type} $lst{'ful'}");

         shell("mv $trd{$set}/trees.$strb{$type} $trv{$type}");
         shell("mv $model{$set}/pdf.$strb{$type} $pdf{$type}");
      }
   }

   # window coefficients
   foreach $type (@cmp) {
      shell("cp $windir/${type}.win* $voice");
   }

   # gv pdfs
   foreach $type (@cmp) {
      shell("cp $gvpdf{$type}.big $voice/gv-${type}.pdf");
   }

   # utt -> label converter
   shell("cp $datdir/scripts/label.feats $voice");
   shell("cp $datdir/scripts/label-full.awk $voice");
}


# hts_engine (synthesizing waveforms using hts_engine)
if ($ENGIN) {
   print_time("synthesizing waveforms using hts_engine");
   
   $dir = "${prjdir}/gen/qst${qnum}/ver${ver}/hts_engine";
   mkdir ${dir}, 0755;
   
   # hts_engine command line & options 
   # model file & trees
   $hts_engine = "$ENGINE -td $trv{'dur'} -tf $trv{'lf0'} -tm $trv{'mgc'} "
                       . "-md $pdf{'dur'} -mf $pdf{'lf0'} -mm $pdf{'mgc'} ";
   
   # window coefficients
   $type = 'mgc';
   for ($d=1;$d<=$nwin{$type};$d++) {
      $hts_engine .= "-dm $voice/$win{$type}[$d-1] ";
   }
   $type = 'lf0';
   for ($d=1;$d<=$nwin{$type};$d++) {
      $hts_engine .= "-df $voice/$win{$type}[$d-1] ";
   }

   # control parameters (sampling rate, frame shift, frequency warping, etc.)
   $lgopt = "-l" if ($lg);
   $hts_engine .= "-s $sr -p $fs -a $fw -g $gm $lgopt -b ".($pf-1.0)." ";
   
   # GV pdfs
   if ($useGV) {
      $hts_engine .= "-cm $voice/gv-mgc.pdf -cf $voice/gv-lf0.pdf ";
      $hts_engine .= "-b 0.0 ";  # turn off postfiltering
   }

   # generate waveform using hts_engine
   open(SCP, $scp{'gen'}) || die "Cannot open $!";
   while (<SCP>) {
      $lab = $_; chomp($lab);
      $base = `basename $lab .lab`; chomp($base);
      
      print "Synthesizing a speech waveform from $lab using hts_engine...";
      shell("$hts_engine -or ${dir}/${base}.raw -ot ${dir}/${base}.trace $lab");
      shell("$SOX -c 1 -s -2 -t raw -r $sr ${dir}/${base}.raw -c 1 -s -2 -t wav -r $sr ${dir}/${base}.wav");
      print "done.\n";
   }
   close(SCP);
}


# HERest (semi-tied covariance matrices)
if ($SEMIT) {
   print_time("semi-tied covariance matrices");

   foreach $set (@SET) {
      shell("cp $reclmmf{$set} $stcmmf{$set}");
   }
   
   $opt = "-C $cfg{'stc'} -K $model{'cmp'} stc -u smvdmv";

   make_config();
   make_stc_base();

   shell("$HERest{'ful'} -H $stcmmf{'cmp'} -N $stcmmf{'dur'} -M $model{'cmp'} -R $model{'dur'} $opt $lst{'ful'} $lst{'ful'}");

   # compress reestimated mmfs
   foreach $set (@SET) {
      shell("gzip -c $stcmmf{$set} > $stcmmf{$set}.embedded.gz");
   }
}


# HHEd (making unseen models (stc))
if ($MKUNS) {
   print_time("making unseen models (stc)");

   foreach $set (@SET) {
      make_edfile_mkunseen($set);
      shell("$HHEd{'trn'} -H $stcmmf{$set} -w $stcammf{$set} $mku{$set} $lst{'ful'}");
   }
}


# HMGenS (generating speech parameter sequences (stc))
if ($PGENS) {
   print_time("generating speech parameter sequences (stc)");

   $mix = 'stc';

   mkdir "${prjdir}/gen/qst${qnum}/ver${ver}/$mix", 0755;
   for ($pgtype=0; $pgtype<=2; $pgtype++) {
      # prepare output directory
      $dir = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype";
      mkdir $dir, 0755;

      # generate parameter
      shell("$HMGenS -c $pgtype -H $stcammf{'cmp'} -N $stcammf{'dur'} -M $dir $tiedlst{'cmp'} $tiedlst{'dur'}");
   }
}


# SPTK (synthesizing waveforms (stc))
if ($WGENS) {
   print_time("synthesizing waveforms (stc)");

   $mix = 'stc';

   mkdir "${prjdir}/gen/qst${qnum}/ver${ver}/$mix", 0755;
   for ($pgtype=0; $pgtype<=2; $pgtype++) {
      gen_wave("${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype");
   }
}


# HHED (increasing the number of mixture components (1mix -> 2mix))
if ($UPMIX) {
   print_time("increasing the number of mixture components (1mix -> 2mix)");

   $set = 'cmp';
   make_edfile_upmix($set);
   shell("$HHEd{'trn'} -H $reclmmf{$set} -w $reclmmf{$set}.2mix $upm{$set} $lst{'ful'}");

   $set = 'dur';
   shell("cp $reclmmf{$set} $reclmmf{$set}.2mix");
}


# fix variables
$reclmmf{'dur'} .= ".2mix";
$reclmmf{'cmp'} .= ".2mix";
$rclammf{'dur'} .= ".2mix";
$rclammf{'cmp'} .= ".2mix";


# HERest (embedded reestimation (2mix))
if ($ERST5) {
   print_time("embedded reestimation (2mix)");

   for ($i=1;$i<=$nIte;$i++) {
      print("\n\nIteration $i of Embedded Re-estimation\n");
      shell("$HERest{'ful'} -H $reclmmf{'cmp'} -N $reclmmf{'dur'} -M $model{'cmp'} -R $model{'dur'} $lst{'ful'} $lst{'ful'}");
   }

   # compress reestimated mmfs
   foreach $set (@SET) {
      shell("gzip -c $reclmmf{$set} > $reclmmf{$set}.embedded.gz");
   }
}


# HHEd (making unseen models (2mix))
if ($MKUN2) {
   print_time("making unseen models (2mix)");

   foreach $set (@SET) {
      make_edfile_mkunseen($set);
      shell("$HHEd{'trn'} -H $reclmmf{$set} -w $rclammf{$set} $mku{$set} $lst{'ful'}");
   }
}


# HMGenS (generating speech parameter sequences (2mix))
if ($PGEN2) {
   print_time("generating speech parameter sequences (2mix)");

   $mix = '2mix';
   
   mkdir "${prjdir}/gen/qst${qnum}/ver${ver}/$mix", 0755;
   for ($pgtype=0; $pgtype<=2; $pgtype++) {
      # prepare output directory 
      $dir = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype";
      mkdir $dir, 0755; 
            
      # generate parameter
      shell("$HMGenS -c $pgtype -H $rclammf{'cmp'} -N $rclammf{'dur'} -M $dir $tiedlst{'cmp'} $tiedlst{'dur'}");
   }
}


# SPTK (synthesizing waveforms (2mix))
if ($WGEN2) {
   print_time("synthesizing waveforms (2mix)");

   $mix = '2mix';

   mkdir "${prjdir}/gen/qst${qnum}/ver${ver}/$mix", 0755;
   for ($pgtype=0; $pgtype<=2; $pgtype++) { 
      gen_wave("${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype");
   }
}


# sub routines ============================
sub shell($) {
   my($command) = @_;
   my($exit);

   $exit = system($command);

   if($exit/256 != 0){
      die "Error in $command\n"
   }
}

sub print_time ($) {
   my($message) = @_;
   my($ruler);
   
   $message .= `date`;
   
   $ruler = '';
   for ($i=0; $i<=length($message)+10; $i++) {
      $ruler .= '=';
   }
   
   print "\n$ruler\n";
   print "Start @_ at ".`date`;
   print "$ruler\n\n";
}

# sub routine for generating proto-type model
sub make_proto {
   my($i, $j, $k, $s);

   # name of proto type definition file
   $prtfile .= "state-${nState}_stream-$nstream{'total'}";
   foreach $type (@cmp) {
      $prtfile .= "_${type}-$vSize{$type}";
   }
   $prtfile .= ".prt";


   # output prototype definition
   # open proto type definition file 
   open(PROTO,">$prtfile") || die "Cannot open $!";

   # output header 
   # output vector size & feature type
   print PROTO "~o <VecSize> $vSize{'total'} <USER> <DIAGC>";
   
   # output information about multi-space probability distribution (MSD)
   print PROTO "<MSDInfo> $nstream{'total'} ";
   foreach $type (@cmp) {
      for ($s=$strb{$type};$s<=$stre{$type};$s++) {
         print PROTO " $msdi{$type} ";
      }
   }
   
   # output information about stream
   print PROTO "<StreamInfo> $nstream{'total'}";
   foreach $type (@cmp) {
      for ($s=$strb{$type};$s<=$stre{$type};$s++) {
         printf PROTO " %d", $vSize{$type}/$nstream{$type};
      }
   }
   print PROTO "\n";

   # output HMMs
   print  PROTO "<BeginHMM>\n";
   printf PROTO "  <NumStates> %d\n", $nState+2;

   # output HMM states 
   for ($i=2;$i<=$nState+1;$i++) {
      # output state information
      print PROTO "  <State> $i\n";

      # output stream weight
      print PROTO "  <SWeights> $nstream{'total'}";
      foreach $type (@cmp) {
         for ($s=$strb{$type};$s<=$stre{$type};$s++) {
            print PROTO " $strw{$type}";
         }
      }
      print PROTO "\n";

      # output stream information
      foreach $type (@cmp) {
         for ($s=$strb{$type};$s<=$stre{$type};$s++) {
            print  PROTO "  <Stream> $s\n";
            if ($msdi{$type}==0) { # non-MSD stream
               # output mean vector 
               printf PROTO "    <Mean> %d\n", $vSize{$type}/$nstream{$type};
               for ($k=1;$k<=$vSize{$type}/$nstream{$type};$k++) {
                  print PROTO "      " if ($k%10==1); 
                  print PROTO "0.0 ";
                  print PROTO "\n" if ($k%10==0);
               }
               print PROTO "\n" if ($k%10!=1);

               # output covariance matrix (diag)
               printf PROTO "    <Variance> %d\n", $vSize{$type}/$nstream{$type};
               for ($k=1;$k<=$vSize{$type}/$nstream{$type};$k++) {
                  print PROTO "      " if ($k%10==1); 
                  print PROTO "1.0 ";
                  print PROTO "\n" if ($k%10==0);
               }
               print PROTO "\n" if ($k%10!=1);
            }	     
            else { # MSD stream 
               # output MSD
               print  PROTO "  <NumMixes> 2\n";

               # output 1st space (non 0-dimensional space)
               # output space weights
               print  PROTO "  <Mixture> 1 0.5000\n";
               
               # output mean vector 
               printf PROTO "    <Mean> %d\n",$vSize{$type}/$nstream{$type};
               for ($k=1;$k<=$vSize{$type}/$nstream{$type};$k++) {
                  print PROTO "      " if ($k%10==1); 
                  print PROTO "0.0 ";
                  print PROTO "\n" if ($k%10==0);
               }
               print PROTO "\n" if ($k%10!=1);

               # output covariance matrix (diag)
               printf PROTO "    <Variance> %d\n", $vSize{$type}/$nstream{$type};
               for ($k=1;$k<=$vSize{$type}/$nstream{$type};$k++) {
                  print PROTO "      " if ($k%10==1); 
                  print PROTO "1.0 ";
                  print PROTO "\n" if ($k%10==0);
               }
               print PROTO "\n" if ($k%10!=1);

               # output 2nd space (0-dimensional space)
               print PROTO "  <Mixture> 2 0.5000\n";
               print PROTO "    <Mean> 0\n";
               print PROTO "    <Variance> 0\n";
            }
         }
      }
   }

   # output state transition matrix
   printf PROTO "  <TransP> %d\n", $nState+2;
   print  PROTO "    ";
   for ($j=1;$j<=$nState+2;$j++) {
      print PROTO "1.000e+0 " if ($j==2);
      print PROTO "0.000e+0 " if ($j!=2);
   }
   print PROTO "\n";
   print PROTO "    ";
   for ($i=2;$i<=$nState+1;$i++) {
      for ($j=1;$j<=$nState+2;$j++) {
         print PROTO "6.000e-1 " if ($i==$j);
         print PROTO "4.000e-1 " if ($i==$j-1);
         print PROTO "0.000e+0 " if ($i!=$j && $i!=$j-1);
      }
      print PROTO "\n";
      print PROTO "    ";
   }
   for ($j=1;$j<=$nState+2;$j++) {
      print PROTO "0.000e+0 ";
   }
   print PROTO "\n";

   # output footer
   print PROTO "<EndHMM>\n";

   close(PROTO);

   # output variance flooring macro for duration model
   open(VF,">$vfloors{'dur'}") || die "Cannot open $!";
   for ($i=1;$i<=$nState;$i++) {
      print VF "~v varFloor$i\n";
      print VF "<Variance> 1\n";
      print VF " 1.0\n"
   }
   close(VF);
}      

# sub routine for generating baseclass for STC
sub make_stc_base {
   my($type,$s,$class);

   # output baseclass definition
   # open baseclass definition file
   open(BASE,">$stcbase{'cmp'}") || die "Cannot open $!";

   # output header
   print BASE "~b \"stc.base\"\n";
   print BASE "<MMFIDMASK> *\n";
   print BASE "<PARAMETERS> MIXBASE\n";

   # output information about stream
   print BASE "<STREAMINFO> $nstream{'total'}";
   foreach $type (@cmp) {
      for ($s=$strb{$type};$s<=$stre{$type};$s++) {
         printf BASE " %d", $vSize{$type}/$nstream{$type};
      }
   }
   print BASE "\n";

   # output number of baseclasses
   $class = 0;
   foreach $type (@cmp) {
      for ($s=$strb{$type};$s<=$stre{$type};$s++) {
         if ($msdi{$type}==0) {
            $class++;
         }
         else {
            $class += 2;
         }
      }
   }
   print BASE "<NUMCLASSES> $class\n";

   # output baseclass pdfs
   $class = 1;
   foreach $type (@cmp) {
      for ($s=$strb{$type};$s<=$stre{$type};$s++) {
         if ($msdi{$type}==0) {
            printf BASE "<CLASS> %d {*.state[2-%d].stream[%d].mix[%d]}\n",$class,$nState+1,$s,1;
            $class++;
         }
         else {
            printf BASE "<CLASS> %d {*.state[2-%d].stream[%d].mix[%d]}\n",$class,  $nState+1,$s,1;
            printf BASE "<CLASS> %d {*.state[2-%d].stream[%d].mix[%d]}\n",$class+1,$nState+1,$s,2;
            $class+=2;
         }
      }
   }
   
   # close file
   close(BASE);
}

# sub routine for generating config files
sub make_config {
   my($s,$type,@boolstring);
   $boolstring[0] = 'FALSE';
   $boolstring[1] = 'TRUE';

   # config file for model training 
   open(CONF,">$cfg{'trn'}") || die "Cannot open $!";
   print CONF "APPLYVFLOOR = T\n";
   print CONF "NATURALREADORDER = T\n";
   print CONF "NATURALWRITEORDER = T\n";
   print CONF "MINLEAFOCC = $minocc\n";
   print CONF "VFLOORSCALESTR = \"Vector $nstream{'total'}";
   foreach $type (@cmp) {
      for ($s=$strb{$type}; $s<=$stre{$type}; $s++) {
         print CONF " $vflr{$type}";
      }
   }
   print CONF "\"\n";
   printf CONF "DURVARFLOORPERCENTILE = %f\n", 100*$vflr{'dur'};
   print CONF "APPLYDURVARFLOOR = T\n";
   print CONF "MAXSTDDEVCOEF = $maxdev\n";
   print CONF "MINDUR = $mindur\n";
   close(CONF);

   # config file for model training (without variance flooring)
   open(CONF,">$cfg{'nvf'}") || die "Cannot open $!";
   print CONF "APPLYVFLOOR = F\n";
   print CONF "DURVARFLOORPERCENTILE = 0.0\n";
   print CONF "APPLYDURVARFLOOR = F\n";
   close(CONF);

   # config file for STC
   open(CONF,">$cfg{'stc'}") || die "Cannot open $!";
   print CONF "MAXSEMITIEDITER = 20\n";
   print CONF "SEMITIEDMACRO   = \"cmp\"\n";
   print CONF "SAVEFULLC = T\n";
   print CONF "BASECLASS = \"$stcbase{'cmp'}\"\n";
   print CONF "TRANSKIND = SEMIT\n";
   print CONF "USEBIAS   = F\n";
   print CONF "ADAPTKIND = BASE\n";
   print CONF "BLOCKSIZE = \"";
   foreach $type (@cmp) {
      for ($s=$strb{$type}; $s<=$stre{$type}; $s++) {
         $bSize  = $vSize{$type}/$nstream{$type}/$nblk{$type};
         print CONF "IntVec $nblk{$type} ";
         for ($b=1; $b<=$nblk{$type}; $b++) {
            print CONF "$bSize ";
         }
      }
   }
   print CONF "\"\n";
   print CONF "BANDWIDTH = \"";
   foreach $type (@cmp) {
      for ($s=$strb{$type}; $s<=$stre{$type}; $s++) {
         $bSize  = $vSize{$type}/$nstream{$type}/$nblk{$type};
         print CONF "IntVec $nblk{$type} ";
         for ($b=1; $b<=$nblk{$type}; $b++) {
            print CONF "1 ";
         }
      }
   }
   print CONF "\"\n";
   close(CONF);

   # config file for model conversion 
   open(CONF,">$cfg{'cnv'}") || die "Cannot open $!";
   print CONF "NATURALREADORDER = T\n";
   print CONF "NATURALWRITEORDER = F\n";  # hts_engine used BIG ENDIAN
   close(CONF);

   # config file for parameter generation
   open(CONF,">$cfg{'syn'}") || die "Cannot open $!";
   print CONF "NATURALREADORDER = T\n";
   print CONF "NATURALWRITEORDER = T\n";
   print CONF "USEALIGN = T\n";
   
   print CONF "PDFSTRSIZE = \"IntVec $nPdfStreams";  # PdfStream structure
   foreach $type (@cmp) {
      print CONF " $nstream{$type}";
   }
   print CONF "\"\n";
   
   print CONF "PDFSTRORDER = \"IntVec $nPdfStreams";  # order of each PdfStream
   foreach $type (@cmp) {
      print CONF " $ordr{$type}";
   }
   print CONF "\"\n";
   
   print CONF "PDFSTREXT = \"StrVec $nPdfStreams";  # filename extension for each PdfStream
   foreach $type (@cmp) {
      print CONF " $type";
   }
   print CONF "\"\n";
   
   print CONF "WINFN = \"";
   foreach $type (@cmp) {
      print CONF "StrVec $nwin{$type} @{$win{$type}} ";  # window coefficients files for each PdfStream
   }
   print CONF "\"\n";
   print CONF "WINDIR = $windir\n";  # directory which stores window coefficients files
   
   print CONF "MAXEMITER = $maxEMiter\n";
   print CONF "EMEPSILON = $EMepsilon\n";
   print CONF "USEGV      = $boolstring[$useGV]\n";
   print CONF "GVMODELMMF = $gvmmf{'cmp'}\n";
   print CONF "GVHMMLIST  = $gvlst{'cmp'}\n";
   print CONF "MAXGVITER  = $maxGViter\n";
   print CONF "GVEPSILON  = $GVepsilon\n";
   print CONF "MINEUCNORM = $minEucNorm\n";
   print CONF "STEPINIT   = $stepInit\n";
   print CONF "STEPINC    = $stepInc\n";
   print CONF "STEPDEC    = $stepDec\n";
   print CONF "HMMWEIGHT  = $hmmWeight\n";
   print CONF "GVWEIGHT   = $gvWeight\n";
   print CONF "OPTKIND    = $optKind\n";
   
   close(CONF);      
}

# sub routine for generating .hed files for decision-tree clustering
sub make_edfile_state($){
   my($type) = @_;
   my(@lines,$i,@nstate);

   $nstate{'cmp'} = $nState;
   $nstate{'dur'} = 1;

   open(QSFILE,"$qs{$type}") || die "Cannot open $!";
   @lines = <QSFILE>;
   close(QSFILE);

   open(EDFILE,">$cxc{$type}") || die "Cannot open $!";
   print EDFILE "// load stats file\n";
   print EDFILE "RO $gam{$type} \"$stats{$t2s{$type}}\"\n\n";   
   print EDFILE "TR 0\n\n";
   print EDFILE "// questions for decision tree-based context clustering\n";
   print EDFILE @lines;
   print EDFILE "TR 3\n\n";
   print EDFILE "// construct decision trees\n";
   for ($i=2;$i<=$nstate{$t2s{$type}}+1;$i++){
      print EDFILE "TB $thr{$type} ${type}_s${i}_ {*.state[${i}].stream[$strb{$type}-$stre{$type}]}\n";
   }
   print EDFILE "\nTR 1\n\n";
   print EDFILE "// output constructed trees\n";
   print EDFILE "ST \"$tre{$type}\"\n";
   close(EDFILE);
}

# sub routine for untying structures
sub make_edfile_untie($){
   my($set) = @_;
   my($type,$i,@nstate);

   $nstate{'cmp'} = $nState;
   $nstate{'dur'} = 1;

   open(EDFILE,">$unt{$set}") || die "Cannot open $!";

   print EDFILE "// untie parameter sharing structure\n";
   foreach $type (@{$ref{$set}}) {
      for($i=2;$i<=$nstate{$set}+1;$i++){
         if ($set eq "dur") {
            print EDFILE "UT {*.state[$i]}\n";
         }
         else {
            if ($strw{$type}>0.0) {
               print EDFILE "UT {*.state[$i].stream[$strb{$type}-$stre{$type}]}\n";
            }
         }
      }
   }

   close(EDFILE);
}

# sub routine to increase the number of mixture components
sub make_edfile_upmix($){
   my($set) = @_;
   my($type,$i,@nstate);

   $nstate{'cmp'} = $nState;
   $nstate{'dur'} = 1;

   open(EDFILE,">$upm{$set}") || die "Cannot open $!";
   
   print EDFILE "// increase the number of mixtures per stream\n";
   foreach $type (@{$ref{$set}}) {
      for($i=2;$i<=$nstate{$set}+1;$i++){
         if ($set eq "dur") {
            print EDFILE "MU +1 {*.state[$i].mix}\n";
         }
         else {
            print EDFILE "MU +1 {*.state[$i].stream[$strb{$type}-$stre{$type}].mix}\n";
         }
      }
   }

   close(EDFILE);
}

# sub routine to convert statistics file for cmp into one for dur
sub convstats {
   open(IN, "$stats{'cmp'}")  || die "Cannot open $!";
   open(OUT,">$stats{'dur'}") || die "Cannot open $!";
   while(<IN>){
      @LINE = split(' ');
      printf OUT ("%4d %14s %4d %4d\n",$LINE[0],$LINE[1],$LINE[2],$LINE[2]);
   }
   close(IN);
   close(OUT);
}

# sub routine for generating .hed files for mmf -> hts_engine conversion
sub make_edfile_convert($){
   my($type) = @_;

   open (EDFILE,">$cnv{$type}") || die "Cannot open $!";
   print EDFILE "\nTR 2\n\n";
   print EDFILE "// load trees for $type\n";
   print EDFILE "LT \"$tre{$type}\"\n\n";

   print EDFILE "// convert loaded trees for hts_engine format\n";
   print EDFILE "CT \"$trd{$t2s{$type}}\"\n\n";

   print EDFILE "// convert mmf for hts_engine format\n";
   print EDFILE "CM \"$model{$t2s{$type}}\"\n";

   close(EDFILE);
}

# sub routine for generating .hed files for making unseen models
sub make_edfile_mkunseen($){
   my($set) = @_;
   my($type);

   open(EDFILE,">$mku{$set}") || die "Cannot open $!";
   print EDFILE "\nTR 2\n\n";
   foreach $type (@{$ref{$set}}) {
      print EDFILE "// load trees for $type\n";
      print EDFILE "LT \"$tre{$type}\"\n\n";
   }

   print EDFILE "// make unseen model\n";
   print EDFILE "AU \"$lst{'all'}\"\n\n";
   print EDFILE "// make model compact\n";
   print EDFILE "CO \"$tiedlst{$set}\"\n\n";

   close(EDFILE);
}

# sub routine for gv_{mgc,lf0}.pdf -> gv.mmf
sub conv_gvpdf2mmf {
   my($vsize, $stream, $data, $PI, @pdf); 
   $PI = 3.14159265358979;
   $vsize = 0;
   
   open(OUT,">$gvmmf{'cmp'}") || die "cannot open file: $gvmmf{'cmp'}";
 
   # output header
   $nGVstr = @cmp;
   printf OUT "~o\n";
   printf OUT "<STREAMINFO> %d ", $nGVstr;
   foreach $type (@cmp) {
      printf OUT "%d ", $ordr{$type};
      $vsize += $ordr{$type};
   }
   printf OUT "\n<VECSIZE> %d <NULLD><USER><DIAGC>\n", $vsize;
   printf OUT "~h \"gv\"\n";
   printf OUT "<BEGINHMM>\n";
   printf OUT "<NUMSTATES> 3\n";
   printf OUT "<STATE> 2\n";
 
   $stream = 1;
   foreach $type (@cmp) {
      open(IN,$gvpdf{$type}) || die "cannot open file: $gvpdf{$type}";
      @STAT=stat(IN);
      read(IN,$data,$STAT[7]);
      close(IN);

      $n = $STAT[7]/4;
      @pdf = unpack("f$n",$data);

      # output stream index
      printf OUT "<Stream> %d\n", $stream;
      
      # output mean
      printf OUT "<Mean> %d\n", $ordr{$type};
      for ($i=0; $i<$ordr{$type}; $i++) {
          $mean = shift(@pdf);
          printf OUT "%e ", $mean;
      }

      # output variance
      printf OUT "\n<Variance> %d\n", $ordr{$type};
      $gConst = $ordr{$type}*log(2*$PI); 
      for ($i=0; $i<$ordr{$type}; $i++) {
         $var = shift(@pdf);
         printf OUT "%e ",$var;
         $gConst += log($var);
      }
      printf OUT "\n<GConst> %e\n", $gConst;
      $stream++;
   }
 
   # output footer
   print OUT "<TRANSP> 3\n";
   print OUT "0 1 0\n";
   print OUT "0 0 1\n";
   print OUT "0 0 0\n";
   print OUT "<ENDHMM>\n";
   
   close(OUT);
   
   # generate gv list
   open(OUT,">$gvlst{'cmp'}") || die "cannot open file: $gvlst{'cmp'}";
   print OUT "gv\n";
   close(OUT);
}

# sub routine for log f0 -> f0 conversion
sub lf02pitch($$) {
   my($base,$gendir) = @_;
   my($t,$T,$data);

   # read log f0 file
   open(IN,"$gendir/${base}.lf0");
   @STAT=stat(IN);
   read(IN,$data,$STAT[7]);
   close(IN);

   # log f0 -> pitch conversion
   $T = $STAT[7]/4;
   @frq = unpack("f$T",$data);
   for ($t=0; $t<$T; $t++) {
      if ($frq[$t] == -1.0e+10) {
         $out[$t] = 0.0;
      } else {
         $out[$t] = $sr/exp($frq[$t]);
      }
   }
   $data = pack("f$T",@out);

   # output data
   open(OUT,">$gendir/${base}.pit");
   print OUT $data;
   close(OUT);
}

# sub routine for formant emphasis in Mel-cepstral domain
sub postfiltering($$) {
   my($base,$gendir) = @_;
   my($i,$line);

   # output postfiltering weight coefficient 
   $line = "echo 1 1 ";
   for ($i=2; $i<$ordr{'mgc'}; $i++) {
      $line .= "$pf ";
   }
   $line .= "| $X2X +af > $gendir/weight";
   shell($line);

   # calculate auto-correlation of original mcep
   $line = "$FREQT -m ".($ordr{'mgc'}-1)." -a $fw -M $co -A 0 < $gendir/${base}.mgc |"
         . "$C2ACR -m $co -M 0 -l $fl > $gendir/${base}.r0";
   shell($line);
         
   # calculate auto-correlation of postfiltered mcep   
   $line = "$VOPR  -m -n ".($ordr{'mgc'}-1)." < $gendir/${base}.mgc $gendir/weight | "
         . "$FREQT    -m ".($ordr{'mgc'}-1)." -a $fw -M $co -A 0 | "
         . "$C2ACR -m $co -M 0 -l $fl > $gendir/${base}.p_r0";
   shell($line);

   # calculate MLSA coefficients from postfiltered mcep 
   $line = "$VOPR -m -n ".($ordr{'mgc'}-1)." < $gendir/${base}.mgc $gendir/weight | "
         . "$MC2B    -m ".($ordr{'mgc'}-1)." -a $fw | "
         . "$BCP     -n ".($ordr{'mgc'}-1)." -s 0 -e 0 > $gendir/${base}.b0";
   shell($line);
   
   # calculate 0.5 * log(acr_orig/acr_post)) and add it to 0th MLSA coefficient     
   $line = "$VOPR -d < $gendir/${base}.r0 $gendir/${base}.p_r0 | "
         . "$SOPR -LN -d 2 | "
         . "$VOPR -a $gendir/${base}.b0 > $gendir/${base}.p_b0";
   shell($line);
   
   # generate postfiltered mcep
   $line = "$VOPR  -m -n ".($ordr{'mgc'}-1)." < $gendir/${base}.mgc $gendir/weight | "
         . "$MC2B     -m ".($ordr{'mgc'}-1)." -a $fw | "
         . "$BCP      -n ".($ordr{'mgc'}-1)." -s 1 -e ".($ordr{'mgc'}-1)." | "
         . "$MERGE    -n ".($ordr{'mgc'}-2)." -s 0 -N 0 $gendir/${base}.p_b0 | "
         . "$B2MC     -m ".($ordr{'mgc'}-1)." -a $fw > $gendir/${base}.p_mgc";
   shell($line);
}

# sub routine for speech synthesis from log f0 and Mel-cepstral coefficients 
sub gen_wave($) {
   my($gendir) = @_;
   my($line,@FILE,$num,$period,$file,$base);

   $line   = `ls $gendir/*.mgc`;
   @FILE   = split('\n',$line);
   $num    = @FILE;
   $lgopt = "-l" if ($lg);

   print "Processing directory $gendir:\n";  
   foreach $file (@FILE) {
      $base = `basename $file .mgc`;
      chomp($base);
      if ( -s $file && -s "$gendir/$base.lf0" ) {
         print " Synthesizing a speech waveform from $base.mgc and $base.lf0...";
         
         # convert log F0 to pitch
         lf02pitch($base,$gendir);
         
         if ($ul) {
            # MGC-LSPs -> MGC coefficients
            $line = "$LSPCHECK -m ".($ordr{'mgc'}-1)." -s ".($sr/1000)." -r $file | "
                  . "$LSP2LPC  -m ".($ordr{'mgc'}-1)." -s ".($sr/1000)." $lgopt | "
                  . "$MGC2MGC  -m ".($ordr{'mgc'}-1)." -a $fw -g $gm -n -u -M ".($ordr{'mgc'}-1)." -A $fw -G $gm "
                  . " > $gendir/$base.c_mgc";
            shell($line);
            
            $mgc = "$gendir/$base.c_mgc";
         }
         else { 
            # apply postfiltering
            if ($gm==0 && $pf!=1.0 && $useGV==0) {
               postfiltering($base,$gendir);
               $mgc = "$gendir/$base.p_mgc";
            }
            else {
               $mgc = $file;
            }
         }
         
         # synthesize waveform
	 # changed option -g for SPTK-3.2 mglsadf with option -c, (-g was used in SPT-3.1)
         $line = "$EXCITE -p $fs $gendir/$base.pit | "
               . "$MGLSADF -m ".($ordr{'mgc'}-1)." -p $fs -a $fw -c $gm $mgc | "
               . "$X2X +fs | "
               . "$SOX -c 1 -s -2 -t raw -r $sr - -c 1 -s -2 -t wav -r $sr $gendir/$base.wav";

         shell($line);
         
         print "done\n";
      }
   }
   print "done\n";
}

##################################################################################################

