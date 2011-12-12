#!/usr/bin/perl
# ----------------------------------------------------------------- #
#           The HMM-Based Speech Synthesis System (HTS)             #
#           developed by HTS Working Group                          #
#           http://hts.sp.nitech.ac.jp/                             #
# ----------------------------------------------------------------- #
#                                                                   #
#  Copyright (c) 2001-2011  Nagoya Institute of Technology          #
#                           Department of Computer Science          #
#                                                                   #
#                2001-2008  Tokyo Institute of Technology           #
#                           Interdisciplinary Graduate School of    #
#                           Science and Engineering                 #
#                                                                   #
#                2008       University of Edinburgh                 #
#                           Centre for Speech Technology Research   #
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

$| = 1;

if ( @ARGV < 1 ) {
   print "usage: Training.pl Config.pm\n";
   exit(0);
}

# load configuration variables
require( $ARGV[0] );

# model structure
foreach $set (@SET) {
   $vSize{$set}{'total'}   = 0;
   $nstream{$set}{'total'} = 0;
   $nPdfStreams{$set}      = 0;
   foreach $type ( @{ $ref{$set} } ) {
      $vSize{$set}{$type} = $nwin{$type} * $ordr{$type};
      $vSize{$set}{'total'} += $vSize{$set}{$type};
      $nstream{$set}{$type} = $stre{$type} - $strb{$type} + 1;
      $nstream{$set}{'total'} += $nstream{$set}{$type};
      $nPdfStreams{$set}++;
   }
}

# File locations =========================
# data directory
$datdir = "$prjdir/data";

# data location file
$scp{'trn'} = "$datdir/scp/train.scp";
$scp{'gen'} = "$datdir/scp/gen.scp";
$scp{'adp'} = "$datdir/scp/adapt.scp";

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
$cfg{'syn'} = "$prjdir/configs/syn.cnf";
$cfg{'adp'} = "$prjdir/configs/adp.cnf";
$cfg{'sat'} = "$prjdir/configs/sat.cnf";
$cfg{'aln'} = "$prjdir/configs/aln.cnf";
$cfg{'map'} = "$prjdir/configs/map.cnf";
foreach $type (@cmp) {
   $cfg{$type} = "$prjdir/configs/${type}.cnf";
}
foreach $type (@dur) {
   $cfg{$type} = "$prjdir/configs/${type}.cnf";
}
foreach $set (@SET) {
   $cfg{'dec'}{$set} = "$prjdir/configs/dec_${set}.cnf";
}

# name of proto type definition file
$prtfile{'cmp'} = "$prjdir/proto/qst${qnum}/ver$ver/state-${nState}_stream-$nstream{'cmp'}{'total'}";
foreach $type (@cmp) {
   $prtfile{'cmp'} .= "_${type}-$vSize{'cmp'}{$type}";
}
$prtfile{'cmp'} .= ".prt";

# model files
foreach $set (@SET) {
   $model{$set}   = "$prjdir/models/qst${qnum}/ver${ver}/${set}";
   $hinit{$set}   = "$model{$set}/HInit";
   $hrest{$set}   = "$model{$set}/HRest";
   $vfloors{$set} = "$model{$set}/vFloors";
   $avermmf{$set} = "$model{$set}/average.mmf";
   $initmmf{$set} = "$model{$set}/init.mmf";
   $monommf{$set} = "$model{$set}/monophone.mmf";
   $fullmmf{$set} = "$model{$set}/fullcontext.mmf";
   $clusmmf{$set} = "$model{$set}/clustered.mmf";
   $untymmf{$set} = "$model{$set}/untied.mmf";
   $reclmmf{$set} = "$model{$set}/re_clustered.mmf";
   $rclammf{$set} = "$model{$set}/re_clustered_all.mmf";
   $spatmmf{$set} = "$model{$set}/re_clustered_sat.mmf";
   $satammf{$set} = "$model{$set}/re_clustered_sat_all.mmf";
   $tiedlst{$set} = "$model{$set}/tiedlist";
}

# statistics files
foreach $set (@SET) {
   $stats{$set} = "$prjdir/stats/qst${qnum}/ver${ver}/${set}.stats";
}

# model edit files
foreach $set (@SET) {
   $hed{$set} = "$prjdir/edfiles/qst${qnum}/ver${ver}/${set}";
   $lvf{$set} = "$hed{$set}/lvf.hed";
   $m2f{$set} = "$hed{$set}/m2f.hed";
   $mku{$set} = "$hed{$set}/mku.hed";
   $unt{$set} = "$hed{$set}/unt.hed";
   $upm{$set} = "$hed{$set}/upm.hed";
   foreach $type ( @{ $ref{$set} } ) {
      $cnv{$type} = "$hed{$set}/cnv_$type.hed";
      $cxc{$type} = "$hed{$set}/cxc_$type.hed";
   }
}

# questions about contexts
foreach $set (@SET) {
   foreach $type ( @{ $ref{$set} } ) {
      $qs{$type}     = "$datdir/questions/questions_qst${qnum}.hed";
      $qs_utt{$type} = "$datdir/questions/questions_utt_qst${qnum}.hed";
   }
}

# decision tree files
foreach $set (@SET) {
   $trd{$set} = "${prjdir}/trees/qst${qnum}/ver${ver}/${set}";
   foreach $type ( @{ $ref{$set} } ) {
      $mdl{$type} = "-m -a $mdlf{$type}" if ( $thr{$type} eq '000' );
      $tre{$type} = "$trd{$set}/${type}.inf";
   }
}

# converted model & tree files for hts_engine
$voice = "$prjdir/voices/qst${qnum}/ver${ver}";
foreach $set (@SET) {
   foreach $type ( @{ $ref{$set} } ) {
      $trv{$type} = "$voice/tree-${type}.inf";
      $pdf{$type} = "$voice/${type}.pdf";
   }
}
$type       = 'lpf';
$trv{$type} = "$voice/tree-${type}.inf";
$pdf{$type} = "$voice/${type}.pdf";

# window files for parameter generation
$windir = "${datdir}/win";
foreach $type (@cmp) {
   for ( $d = 1 ; $d <= $nwin{$type} ; $d++ ) {
      $win{$type}[ $d - 1 ] = "${type}.win${d}";
   }
}
$type                 = 'lpf';
$d                    = 1;
$win{$type}[ $d - 1 ] = "${type}.win${d}";

# global variance files and directories for parameter generation
$gvdir         = "$prjdir/gv/qst${qnum}/ver${ver}";
$gvfaldir      = "$gvdir/fal";
$gvdatdir      = "$gvdir/dat";
$gvlabdir      = "$gvdir/lab";
$scp{'gv'}     = "$gvdir/gv.scp";
$mlf{'gv'}     = "$gvdir/gv.mlf";
$prtfile{'gv'} = "$gvdir/state-1_stream-${nPdfStreams{'cmp'}}";
foreach $type (@cmp) {
   $prtfile{'gv'} .= "_${type}-$ordr{$type}";
}
$prtfile{'gv'} .= ".prt";
$avermmf{'gv'} = "$gvdir/average.mmf";
$fullmmf{'gv'} = "$gvdir/fullcontext.mmf";
$clusmmf{'gv'} = "$gvdir/clustered.mmf";
$clsammf{'gv'} = "$gvdir/clustered_all.mmf";
$tiedlst{'gv'} = "$gvdir/tiedlist";
$mku{'gv'}     = "$gvdir/mku.hed";

foreach $type (@cmp) {
   $gvcnv{$type} = "$gvdir/cnv_$type.hed";
   $gvcxc{$type} = "$gvdir/cxc_$type.hed";
   $gvmdl{$type} = "-m -a $gvmdlf{$type}" if ( $gvthr{$type} eq '000' );
   $gvtre{$type} = "$gvdir/${type}.inf";
   $gvpdf{$type} = "$voice/gv-${type}.pdf";
   $gvtrv{$type} = "$voice/tree-gv-${type}.inf";
}

# adaptation-related files
foreach $set (@SET) {
   $regtree{$set} = "$model{$set}/regTrees";
   $xforms{$set}  = "$model{$set}/xforms";
   $mapmmf{$set}  = "$model{$set}/mapmmf";
   for $type ( 'reg', 'dec' ) {    # reg -> regression tree, dec -> decision tree
      $red{$set}{$type}   = "$hed{$set}/${type}.hed";
      $rbase{$set}{$type} = "$regtree{$set}/${type}.base";
      $rtree{$set}{$type} = "$regtree{$set}/${type}.tree";
   }
}

# HTS Commands & Options ========================
$HCompV{'cmp'} = "$HCOMPV    -A    -C $cfg{'trn'} -D -T 1 -S $scp{'trn'} -m ";
$HCompV{'gv'}  = "$HCOMPV    -A    -C $cfg{'trn'} -D -T 1 -S $scp{'gv'}  -m ";
$HList         = "$HLIST     -A    -C $cfg{'trn'} -D -T 1 -S $scp{'trn'} -h -z ";
$HInit         = "$HINIT     -A    -C $cfg{'trn'} -D -T 1 -S $scp{'trn'}                -m 1 -u tmvw     -w $wf ";
$HRest         = "$HREST     -A    -C $cfg{'trn'} -D -T 1 -S $scp{'trn'}                -m 1 -u tmvw     -w $wf ";
$HERest{'mon'} = "$HEREST    -A    -C $cfg{'trn'} -D -T 1 -S $scp{'trn'} -I $mlf{'mon'} -m 1 -u tmvwdmv  -w $wf -t $beam ";
$HERest{'ful'} = "$HEREST    -A -B -C $cfg{'trn'} -D -T 1 -S $scp{'trn'} -I $mlf{'ful'} -m 1 -u tmvwdmv  -w $wf -t $beam -h $spkrPat ";
$HERest{'gv'}  = "$HEREST    -A    -C $cfg{'trn'} -D -T 1 -S $scp{'gv'}  -I $mlf{'gv'}  -m 1 ";
$HERest{'adp'} = "$HEREST    -A -B -C $cfg{'trn'} -D -T 1 -S $scp{'adp'} -I $mlf{'ful'} -m 1 -u ada      -w $wf -t $beam -h $spkrPat ";
$HERest{'map'} = "$HEREST    -A -B -C $cfg{'trn'} -D -T 1 -S $scp{'adp'} -I $mlf{'ful'} -m 1 -u pmvwdpmv -w $wf -t $beam -h $spkrPat ";
$HHEd{'trn'}   = "$HHED      -A  -C $cfg{'trn'} -D -T 1 -p -i ";
$HHEd{'cnv'}   = "$HHED      -A -B -C $cfg{'cnv'} -D -T 1 -p -i ";
$HSMMAlign     = "$HSMMALIGN -A    -C $cfg{'trn'} -D -T 1 -S $scp{'trn'} -I $mlf{'mon'} -t $beam -w 1.0 ";
$HMGenS        = "$HMGENS    -A -B -C $cfg{'syn'} -D -T 1 -S $scp{'gen'} -t $beam -h $spkrPat ";

# =============================================================
# ===================== Main Program ==========================
# =============================================================

# preparing environments
if ($MKEMV) {
   print_time("preparing environments");

   # make directories
   foreach $dir ( 'models', 'stats', 'edfiles', 'trees', 'gv', 'voices', 'gen', 'proto' ) {
      mkdir "$prjdir/$dir",                      0755;
      mkdir "$prjdir/$dir/qst${qnum}",           0755;
      mkdir "$prjdir/$dir/qst${qnum}/ver${ver}", 0755;
   }
   foreach $set (@SET) {
      mkdir "$model{$set}",   0755;
      mkdir "$hinit{$set}",   0755;
      mkdir "$hrest{$set}",   0755;
      mkdir "$hed{$set}",     0755;
      mkdir "$trd{$set}",     0755;
      mkdir "$regtree{$set}", 0755;
      mkdir "$xforms{$set}",  0755;
      mkdir "$mapmmf{$set}",  0755;
   }

   # make config files
   mkdir "$prjdir/configs", 0755;
   make_config();

   # make model prototype definition file
   make_proto();
}

# HCompV (computing variance floors)
if ($HCMPV) {
   print_time("computing variance floors");

   # make average model and compute variance floors
   shell("$HCompV{'cmp'} -M $model{'cmp'} -o $avermmf{'cmp'} $prtfile{'cmp'}");
   shell("head -n 1 $prtfile{'cmp'} > $initmmf{'cmp'}");
   shell("cat $vfloors{'cmp'} >> $initmmf{'cmp'}");

   shell("$HList | $TEE $model{'cmp'}/HList");
   $dsum1 = 0.0;
   $dsum2 = 0.0;
   $dnum  = 0;
   open( LOG, "$model{'cmp'}/HList" ) || die "Cannot open $!";
   while ( $str = <LOG> ) {
      if ( index( $str, " Source: " ) >= 0 ) {
         $str = substr( $str, index( $str, " Source: " ) + 9 );
         while ( index( $str, " " ) >= 0 || index( $str, "\t" ) >= 0 ) { substr( $str, -1, 1 ) = ""; }
         $sbase = `dirname $str`;
         chomp($sbase);
         $sbase = `basename $sbase`;
         chomp($sbase);
         $base = `basename $str .cmp`;
         chomp($base);
      }
      elsif ( index( $str, "Num Samples:" ) >= 0 ) {
         $nframe = substr( $str, 14, index( $str, "File Format:" ) - 14 );
         $nlab = `cat $datdir/labels/mono/$sbase/$base.lab | $WC -l`;
         chomp($nlab);
         $dtmp = $nframe / ( $nlab * $nState );
         $dsum1 += $dtmp;
         $dsum2 += $dtmp * $dtmp;
         $dnum++;
      }
   }
   close(LOG);
   $dmean = $dsum1 / $dnum;
   $dvari = $dsum2 / $dnum - $dmean * $dmean;

   make_duration_vfloor( $dmean, $dvari );
}

# HInit & HRest (initialization & reestimation)
if ($IN_RE) {
   print_time("initialization & reestimation");

   if ($daem) {
      open( LIST, $lst{'mon'} ) || die "Cannot open $!";
      while ( $phone = <LIST> ) {

         # trimming leading and following whitespace characters
         $phone =~ s/^\s+//;
         $phone =~ s/\s+$//;

         # skip a blank line
         if ( $phone eq '' ) {
            next;
         }

         print "=============== $phone ================\n";
         print "use average model instead of $phone\n";
         foreach $set (@SET) {
            open( SRC, "$avermmf{$set}" )       || die "Cannot open $!";
            open( TGT, ">$hrest{$set}/$phone" ) || die "Cannot open $!";
            while ( $str = <SRC> ) {
               if ( index( $str, "~h" ) == 0 ) {
                  print TGT "~h \"$phone\"\n";
               }
               else {
                  print TGT "$str";
               }
            }
            close(TGT);
            close(SRC);
         }
      }
      close(LIST);
   }
   else {
      open( LIST, $lst{'mon'} ) || die "Cannot open $!";
      while ( $phone = <LIST> ) {

         # trimming leading and following whitespace characters
         $phone =~ s/^\s+//;
         $phone =~ s/\s+$//;

         # skip a blank line
         if ( $phone eq '' ) {
            next;
         }
         $lab = $mlf{'mon'};

         if ( grep( $_ eq $phone, keys %mdcp ) <= 0 ) {
            print "=============== $phone ================\n";
            shell("$HInit -H $initmmf{'cmp'} -M $hinit{'cmp'} -I $lab -l $phone -o $phone $prtfile{'cmp'}");
            shell("$HRest -H $initmmf{'cmp'} -M $hrest{'cmp'} -I $lab -l $phone -g $hrest{'dur'}/$phone $hinit{'cmp'}/$phone");
         }
      }
      close(LIST);

      open( LIST, $lst{'mon'} ) || die "Cannot open $!";
      while ( $phone = <LIST> ) {

         # trimming leading and following whitespace characters
         $phone =~ s/^\s+//;
         $phone =~ s/\s+$//;

         # skip a blank line
         if ( $phone eq '' ) {
            next;
         }

         if ( grep( $_ eq $phone, keys %mdcp ) > 0 ) {
            print "=============== $phone ================\n";
            print "use $mdcp{$phone} instead of $phone\n";
            foreach $set (@SET) {
               open( SRC, "$hrest{$set}/$mdcp{$phone}" ) || die "Cannot open $!";
               open( TGT, ">$hrest{$set}/$phone" )       || die "Cannot open $!";
               while (<SRC>) {
                  s/~h \"$mdcp{$phone}\"/~h \"$phone\"/;
                  print TGT;
               }
               close(TGT);
               close(SRC);
            }
         }
      }
      close(LIST);
   }
}

# HHEd (making a monophone mmf)
if ($MMMMF) {
   print_time("making a monophone mmf");

   foreach $set (@SET) {
      open( EDFILE, ">$lvf{$set}" ) || die "Cannot open $!";

      # load variance floor macro
      print EDFILE "// load variance flooring macro\n";
      print EDFILE "FV \"$vfloors{$set}\"\n";

      # tie stream weight macro
      foreach $type ( @{ $ref{$set} } ) {
         if ( $strw{$type} != 1.0 ) {
            print EDFILE "// tie stream weights\n";
            printf EDFILE "TI SW_all {*.state[%d-%d].weights}\n", 2, $nState + 1;
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

   if ($daem) {
      for ( $i = 1 ; $i <= $daem_nIte ; $i++ ) {
         for ( $j = 1 ; $j <= $nIte ; $j++ ) {

            # embedded reestimation
            $k = $j + ( $i - 1 ) * $nIte;
            print("\n\nIteration $k of Embedded Re-estimation\n");
            $k = ( $i / $daem_nIte )**$daem_alpha;
            shell("$HERest{'mon'} -k $k -H $monommf{'cmp'} -N $monommf{'dur'} -M $model{'cmp'} -R $model{'dur'} $lst{'mon'} $lst{'mon'}");
         }
      }
   }
   else {
      for ( $i = 1 ; $i <= $nIte ; $i++ ) {

         # embedded reestimation
         print("\n\nIteration $i of Embedded Re-estimation\n");
         shell("$HERest{'mon'} -H $monommf{'cmp'} -N $monommf{'dur'} -M $model{'cmp'} -R $model{'dur'} $lst{'mon'} $lst{'mon'}");
      }
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
      open( EDFILE, ">$m2f{$set}" ) || die "Cannot open $!";
      open( LIST,   "$lst{'mon'}" ) || die "Cannot open $!";

      print EDFILE "// copy monophone models to fullcontext ones\n";
      print EDFILE "CL \"$lst{'ful'}\"\n\n";    # CLone monophone to fullcontext

      print EDFILE "// tie state transition probability\n";
      while ( $phone = <LIST> ) {

         # trimming leading and following whitespace characters
         $phone =~ s/^\s+//;
         $phone =~ s/\s+$//;

         # skip a blank line
         if ( $phone eq '' ) {
            next;
         }
         print EDFILE "TI T_${phone} {*-${phone}+*.transP}\n";    # TIe transition prob
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

# HHEd (tree-based context clustering)
if ($CXCL1) {
   print_time("tree-based context clustering");

   # convert cmp stats to duration ones
   convstats();

   # tree-based clustering
   foreach $set (@SET) {
      shell("cp $fullmmf{$set} $clusmmf{$set}");

      $footer = "";
      foreach $type ( @{ $ref{$set} } ) {
         if ( $strw{$type} > 0.0 ) {
            make_edfile_state($type);
            shell("$HHEd{'trn'} -C $cfg{$type} -H $clusmmf{$set} $mdl{$type} -w $clusmmf{$set} $cxc{$type} $lst{'ful'}");
            $footer .= "_$type";
            shell("gzip -c $clusmmf{$set} > $clusmmf{$set}$footer.gz");
         }
      }
   }
}

# HERest (embedded reestimation (clustered))
if ($ERST2) {
   print_time("embedded reestimation (clustered)");

   for ( $i = 1 ; $i <= $nIte ; $i++ ) {
      print("\n\nIteration $i of Embedded Re-estimation\n");
      shell("$HERest{'ful'} -H $clusmmf{'cmp'} -N $clusmmf{'dur'} -M $model{'cmp'} -R $model{'dur'} $lst{'ful'} $lst{'ful'}");
   }

   # compress reestimated mmfs
   foreach $set (@SET) {
      shell("gzip -c $clusmmf{$set} > $clusmmf{$set}.embedded.gz");
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
   foreach $type ( @{ $ref{$set} } ) {
      $tre{$type} .= ".untied";
      $cxc{$type} .= ".untied";
   }
}

# HERest (embedded reestimation (untied))
if ($ERST3) {
   print_time("embedded reestimation (untied)");

   $opt = "-C $cfg{'nvf'} -s $stats{'cmp'} -w 0.0";

   print("\n\nEmbedded Re-estimation for untied mmfs\n");
   shell("$HERest{'ful'} -H $untymmf{'cmp'} -N $untymmf{'dur'} -M $model{'cmp'} -R $model{'dur'} $opt $lst{'ful'} $lst{'ful'}");
}

# HHEd (tree-based context clustering)
if ($CXCL2) {
   print_time("tree-based context clustering");

   # convert cmp stats to duration ones
   convstats();

   # tree-based clustering
   foreach $set (@SET) {
      shell("cp $untymmf{$set} $reclmmf{$set}");

      $footer = "";
      foreach $type ( @{ $ref{$set} } ) {
         make_edfile_state($type);
         shell("$HHEd{'trn'} -C $cfg{$type} -H $reclmmf{$set} $mdl{$type} -w $reclmmf{$set} $cxc{$type} $lst{'ful'}");

         $footer .= "_$type";
         shell("gzip -c $reclmmf{$set} > $reclmmf{$set}$footer.gz");
      }
      shell("gzip -c $reclmmf{$set} > $reclmmf{$set}.nonembedded.gz");
   }
}

# fix variables
$stats{'cmp'} =~ s/untied/re-clustered/;

# HERest (embedded reestimation (re-clustered))
if ($ERST4) {
   print_time("embedded reestimation (re-clustered)");

   for ( $i = 1 ; $i <= $nIte ; $i++ ) {
      if ( $i == $nIte ) {
         $opt = "-s $stats{'cmp'}";
      }
      else {
         $opt = "";
      }
      print("\n\nIteration $i of Embedded Re-estimation\n");
      shell("$HERest{'ful'} -H $reclmmf{'cmp'} -N $reclmmf{'dur'} -M $model{'cmp'} -R $model{'dur'} $opt $lst{'ful'} $lst{'ful'}");
   }

   # compress reestimated mmfs
   foreach $set (@SET) {
      shell("gzip -c $reclmmf{$set} > $reclmmf{$set}.embedded.gz");
   }
}

# HSMMAlign (forced alignment for no-silent GV)
if ($FALGN) {
   print_time("forced alignment for no-silent GV");

   if ( $useGV && $nosilgv && @slnt > 0 ) {

      # make directory
      mkdir "$gvfaldir", 0755;

      # forced alignment
      shell("$HSMMAlign -H $monommf{'cmp'} -N $monommf{'dur'} -m $gvfaldir $lst{'mon'} $lst{'mon'}");
   }
}

# making global variance
if ($MCDGV) {
   print_time("making global variance");

   if ($useGV) {

      # make directories
      mkdir "$gvdatdir", 0755;
      mkdir "$gvlabdir", 0755;

      # make proto
      make_proto_gv();

      # make training data, labels, scp, list, and mlf
      make_data_gv();

      # make average model
      shell("$HCompV{'gv'} -o average.mmf -M $gvdir $prtfile{'gv'}");

      if ($cdgv) {

         # make full context depdent model
         copy_aver2full_gv();
         shell("$HERest{'gv'} -C $cfg{'nvf'} -s $gvdir/gv.stats -w 0.0 -H $fullmmf{'gv'} -M $gvdir $gvdir/gv.list");

         # context-clustering
         my $s = 1;
         shell("cp $fullmmf{'gv'} $clusmmf{'gv'}");
         foreach $type (@cmp) {
            make_edfile_state_gv( $type, $s );
            shell("$HHEd{'trn'} -H $clusmmf{'gv'} $gvmdl{$type} -w $clusmmf{'gv'} $gvcxc{$type} $gvdir/gv.list");
            $s++;
         }

         # re-estimation
         shell("$HERest{'gv'} -H $clusmmf{'gv'} -M $gvdir $gvdir/gv.list");
      }
      else {
         copy_aver2clus_gv();
      }
   }
}

# HHEd (making unseen models (GV))
if ($MKUNG) {
   print_time("making unseen models (GV)");

   if ($useGV) {
      if ($cdgv) {
         make_edfile_mkunseen_gv();
         shell("$HHEd{'trn'} -H $clusmmf{'gv'} -w $clsammf{'gv'} $mku{'gv'} $gvdir/gv.list");
      }
      else {
         copy_clus2clsa_gv();
      }
   }
}

# HHEd (making unseen models (speaker independent))
if ($MKUN1) {
   print_time("making unseen models (speaker independent)");

   foreach $set (@SET) {
      make_edfile_mkunseen($set);
      shell("$HHEd{'trn'} -H $reclmmf{$set} -w $rclammf{$set} $mku{$set} $lst{'ful'}");
   }
}

# HMGenS (generating speech parameter sequences (speaker independent))
if ($PGEN1) {
   print_time("generating speech parameter sequences (speaker independent)");

   $mix = 'SI';
   $dir = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype";
   mkdir "${prjdir}/gen/qst${qnum}/ver${ver}/$mix", 0755;
   mkdir $dir, 0755;

   # generate parameter
   shell("$HMGenS -c $pgtype -H $rclammf{'cmp'} -N $rclammf{'dur'} -M $dir $tiedlst{'cmp'} $tiedlst{'dur'}");
}

# SPTK (synthesizing waveforms (speaker independent))
if ($WGEN1) {
   print_time("synthesizing waveforms (speaker independent)");

   $mix = 'SI';
   $dir = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype";

   gen_wave($dir);}


# HHEd (building regression-class trees for adaptation)
if ($REGTR) {
   print_time("building regression-class trees for adaptation");

   foreach $set (@SET) {
      foreach $type ( 'reg', 'dec' ) {    # reg -> regression tree (k-means),  dec -> decision tree
         make_edfile_regtree( $type, $set );
         shell("$HHEd{'trn'} -C $cfg{'dec'}{$set} -H $rclammf{$set} -M $regtree{$set} $red{$set}{$type} $tiedlst{$set}");
      ###shell("$HHEd{'trn'} -C $cfg{'dec'}{$set} -H $reclmmf{$set} -M $regtree{$set} $red{$set}{$type} $lst{'ful'}");
      }
   }
}

# HERest (speaker adaptation (speaker independent))
if ($ADPT1) {
   print_time("speaker adaptation (speaker independent)");

   for ( $i = 1 ; $i <= $nAdapt ; $i++ ) {
      print("\n\nIteration $i of adaptation transform reestimation\n");

      # Reestimate transform
      $type = $tknd{'adp'};
      $mllr = $tran{'adp'};
      $mix  = "SI+${type}_${mllr}${i}";

      $opt = "-C $cfg{'adp'} ";
      $opt .= "-K $xforms{'cmp'} ${mix} -H $rbase{'cmp'}{$type} -H $rtree{'cmp'}{$type} ";
      $opt .= "-Z $xforms{'dur'} ${mix} -N $rbase{'dur'}{$type} -N $rtree{'dur'}{$type} ";

      if ( $i > 1 ) {
         $mix2 = "SI+${type}_${mllr}" . ( ${i} - 1 );
         $opt .= "-J $xforms{'cmp'} ${mix2} -Y $xforms{'dur'} ${mix2} -a -b ";
      }

      make_config_adapt( $type, $mllr );

      shell("$HERest{'adp'} -H $rclammf{'cmp'} -N $rclammf{'dur'} $opt $tiedlst{'cmp'} $tiedlst{'dur'}");
   }
}

# HERest (speaker adaptation (SI+MLLR+MAP))
if ( $MAPE1 && $addMAP ) {
   print_time("speaker adaptation (SI+MLLR+MAP)");

   $opt = "-C $cfg{'map'} ";

   if ( $nAdapt > 0 ) {
      $type = $tknd{'adp'};
      $mllr = $tran{'adp'};
      $mix  = "SI+${type}_${mllr}${nAdapt}";
      $opt .= "-H $rbase{'cmp'}{$type} -H $rtree{'cmp'}{$type} -J $xforms{'cmp'} ${mix} -E $xforms{'cmp'} ${mix} -a ";
      $opt .= "-N $rbase{'dur'}{$type} -N $rtree{'dur'}{$type} -Y $xforms{'dur'} ${mix} -W $xforms{'dur'} ${mix} -b ";
   }

   make_config_map();

   shell("$HERest{'map'} -H $rclammf{'cmp'} -N $rclammf{'dur'} -M $mapmmf{'cmp'} -R $mapmmf{'dur'} $opt $tiedlst{'cmp'} $tiedlst{'dur'}");
}

if ($addMAP) {
   for $set (@SET) {
      $rclammf{$set} = "$mapmmf{$set}/re_clustered_all.mmf";
   }
}

# HMGenS (generating speech parameter sequences (speaker adapted))
if ($PGEN2) {
   print_time("generating speech parameter sequences (speaker adapted)");

   $type = $tknd{'adp'};
   $mllr = $tran{'adp'};
   $mix  = "SI+${type}_${mllr}${nAdapt}";
   $dir  = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype";

   mkdir "${prjdir}/gen/qst${qnum}/ver${ver}/$mix", 0755;
   mkdir $dir, 0755;

   $opt = "-a -J $xforms{'cmp'} ${mix} -H $rbase{'cmp'}{$type} -H $rtree{'cmp'}{$type} ";
   $opt .= "-b -Y $xforms{'dur'} ${mix} -N $rbase{'dur'}{$type} -N $rtree{'dur'}{$type} ";

   # generate parameter
   shell("$HMGenS -c $pgtype -H $rclammf{'cmp'} -N $rclammf{'dur'} -M $dir ${opt} $tiedlst{'cmp'} $tiedlst{'dur'}");
}

# SPTK (synthesizing waveforms (speaker adapted))
if ($WGEN2) {
   print_time("synthesizing waveforms (speaker adapted)");

   $type = $tknd{'adp'};
   $mllr = $tran{'adp'};
   $mix  = "SI+${type}_${mllr}${nAdapt}";
   $dir  = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype";

   gen_wave($dir);
}

# HERest (Speaker adaptive training (SAT))
if ($SPKAT) {
   print_time("Speaker adaptive training (SAT)");

   for $set (@SET) {
      shell("cp $rclammf{$set} $spatmmf{$set}");
   ###shell("cp $reclmmf{$set} $spatmmf{$set}");
   }

   $type = $tknd{'sat'};
   $mllr = 'sat';

   for ( $i = 1 ; $i <= $nSAT ; $i++ ) {
      print("\n\nIteration $i of Speaker Adaptive Training\n");

      # Reestimate transform
      $mix = "${type}_${mllr}${i}";
      $opt = "-u ada -C $cfg{'sat'} ";
      $opt .= "-K $xforms{'cmp'} ${mix} -H $rbase{'cmp'}{$type} -H $rtree{'cmp'}{$type} ";
      $opt .= "-Z $xforms{'dur'} ${mix} -N $rbase{'dur'}{$type} -N $rtree{'dur'}{$type} ";
      if ( $i > 1 ) {
         $mix2 = "${type}_${mllr}" . ( ${i} - 1 );
         $opt .= "-J $xforms{'cmp'} ${mix2} -Y $xforms{'dur'} ${mix2} -a -b ";
      }

      make_config_adapt( $type, $mllr );

      print("\nEstimating transform for iteration $i\n");
         shell("$HERest{'ful'} -H $spatmmf{'cmp'} -N $spatmmf{'dur'} $opt $tiedlst{'cmp'} $tiedlst{'dur'}");
      ###shell("$HERest{'ful'} -H $spatmmf{'cmp'} -N $spatmmf{'dur'} $opt $lst{'ful'} $lst{'ful'}");

      # Reestimate HMM and duration models
      $opt = "-C $cfg{'sat'} ";
      $opt .= "-a -J $xforms{'cmp'} ${mix} -E $xforms{'cmp'} ${mix} -H $rbase{'cmp'}{$type} -H $rtree{'cmp'}{$type} ";
      $opt .= "-b -Y $xforms{'dur'} ${mix} -W $xforms{'dur'} ${mix} -N $rbase{'dur'}{$type} -N $rtree{'dur'}{$type} ";

      make_config_adapt( $type, $mllr );

      for ( $j = 1 ; $j <= $nIte ; $j++ ) {
         print("\n\nIteration $j of Embedded Re-estimation in SAT\n");
            shell("$HERest{'ful'} -H $spatmmf{'cmp'} -N $spatmmf{'dur'} -M $model{'cmp'} -R $model{'dur'} $opt $tiedlst{'cmp'} $tiedlst{'dur'}");
         ###shell("$HERest{'ful'} -H $spatmmf{'cmp'} -N $spatmmf{'dur'} -M $model{'cmp'} -R $model{'dur'} $opt $lst{'ful'} $lst{'ful'}");
      }

      # compress reestimated mmfs
      for $set (@SET) {
         shell("gzip -c $spatmmf{$set} > $spatmmf{$set}.${i}.embedded.gz");
      }
   }
}

# HHEd (making unseen models (SAT))
if ($MKUN2) {
   print_time("making unseen models (SAT)");

   foreach $set (@SET) {
      make_edfile_mkunseen($set);
      shell("$HHEd{'trn'} -H $spatmmf{$set} -w $satammf{$set} $mku{$set} $tiedlst{$set}");
   ###shell("$HHEd{'trn'} -H $spatmmf{$set} -w $satammf{$set} $mku{$set} $lst{'ful'}");
   }
}

# HMGenS (generating speech parameter sequences (SAT))
if ($PGEN3) {
   print_time("generating speech parameter sequences (SAT)");

   $mix = 'SAT';
   $dir = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype";
   mkdir "${prjdir}/gen/qst${qnum}/ver${ver}/$mix", 0755;
   mkdir $dir, 0755;

   # generate parameter
   shell("$HMGenS -c $pgtype -H $satammf{'cmp'} -N $satammf{'dur'} -M $dir $tiedlst{'cmp'} $tiedlst{'dur'}");
}

# SPTK (synthesizing waveforms (SAT))
if ($WGEN3) {
   print_time("synthesizing waveforms (SAT)");

   $mix = 'SAT';
   $dir = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype";

   gen_wave($dir);
}

# HERest (speaker adaptation (SAT))
if ($ADPT2) {
   print_time("speaker adaptation (SAT)");

   for ( $i = 1 ; $i <= $nAdapt ; $i++ ) {
      print("\n\nIteration $i of adaptation transform estimation\n");

      # Reestimate transform
      $type = $tknd{'adp'};
      $mllr = $tran{'sat'};
      $mix  = "SAT+${type}_${mllr}${i}";

      $opt = "-C $cfg{'adp'} ";
      $opt .= "-K $xforms{'cmp'} ${mix} -H $rbase{'cmp'}{$type} -H $rtree{'cmp'}{$type} ";
      $opt .= "-Z $xforms{'dur'} ${mix} -N $rbase{'dur'}{$type} -N $rtree{'dur'}{$type} ";

      if ( $i > 1 ) {
         $mix2 = "SAT+${type}_${mllr}" . ( ${i} - 1 );
         $opt .= "-J $xforms{'cmp'} ${mix2} -Y $xforms{'dur'} ${mix2} -a -b ";
      }
      else {
         make_config_align();
         $opt .= "-C $cfg{'aln'}";
      }

      make_config_adapt( $type, $mllr );

      shell("$HERest{'adp'} -H $satammf{'cmp'} -N $satammf{'dur'} $opt $tiedlst{'cmp'} $tiedlst{'dur'}");
   }
}

# HERest (speaker adaptation (SAT+MLLR+MAP))
if ( $MAPE2 && $addMAP ) {
   print_time("speaker adaptation (MAP estimation)");

   $opt = "-C $cfg{'map'} ";

   if ( $nAdapt > 0 ) {
      $type = $tknd{'adp'};
      $mllr = $tran{'sat'};
      $mix  = "SAT+${type}_${mllr}${nAdapt}";
      $opt .= "-H $rbase{'cmp'}{$type} -H $rtree{'cmp'}{$type} -J $xforms{'cmp'} ${mix} -E $xforms{'cmp'} ${mix} -a ";
      $opt .= "-N $rbase{'dur'}{$type} -N $rtree{'dur'}{$type} -Y $xforms{'dur'} ${mix} -W $xforms{'dur'} ${mix} -b ";
   }

   make_config_map();

   shell("$HERest{'map'} -H $satammf{'cmp'} -N $satammf{'dur'} -M $mapmmf{'cmp'} -R $mapmmf{'dur'} $opt $tiedlst{'cmp'} $tiedlst{'dur'}");
}

if ($addMAP) {
   for $set (@SET) {
      $satammf{$set} = "$mapmmf{$set}/re_clustered_sat_all.mmf";
   }
}

# HMGenS (generate speech parameter sequences (SAT+adaptation))
if ($PGEN4) {
   print_time("generate speech parameter sequences (SAT+adaptation)");

   $type = $tknd{'adp'};
   $mllr = $tran{'sat'};
   $mix  = "SAT+${type}_${mllr}${nAdapt}";
   $dir  = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype";

   mkdir "${prjdir}/gen/qst${qnum}/ver${ver}/$mix", 0755;
   mkdir $dir, 0755;

   $opt = "-a -J $xforms{'cmp'} ${mix} -H $rbase{'cmp'}{$type} -H $rtree{'cmp'}{$type} ";
   $opt .= "-b -Y $xforms{'dur'} ${mix} -N $rbase{'dur'}{$type} -N $rtree{'dur'}{$type} ";

   # generate parameters
   shell("$HMGenS -c $pgtype -H $satammf{'cmp'} -N $satammf{'dur'} -M $dir ${opt} $tiedlst{'cmp'} $tiedlst{'dur'}");
}

# SPTK (synthesizing waveforms (SAT+adaptation))
if ($WGEN4) {
   print_time("synthesizing waveforms (SAT+adaptation)");

   $type = $tknd{'adp'};
   $mllr = $tran{'sat'};
   $mix  = "SAT+${type}_${mllr}${nAdapt}";
   $dir  = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/$pgtype";

   gen_wave($dir);
}

# HHEd (converting mmfs to the hts_engine file format)
if ($CONVM) {
   print_time("converting mmfs to the hts_engine file format");

   $tran = $tknd{'adp'};
   $mllr = $tran{'sat'};
   $mix  = "SAT+${tran}_${mllr}${nAdapt}";

   # models and trees
   foreach $set (@SET) {
      $spxf = "$xforms{$set}/${spkr}.${mix}";
      $mmfs = "-H $rbase{$set}{$tran} -H $rtree{$set}{$tran}";
      foreach $type ( @{ $ref{$set} } ) {
         make_edfile_convert( $type, $spxf );
         shell("$HHEd{'cnv'} -H $satammf{$set} $mmfs $cnv{$type} $tiedlst{$set}");
         shell("mv $trd{$set}/trees.$strb{$type} $trv{$type}");
         shell("mv $model{$set}/pdf.$strb{$type} $pdf{$type}");
      }
   }

   # window coefficients
   foreach $type (@cmp) {
      shell("cp $windir/${type}.win* $voice");
   }

   # gv pdfs
   if ($useGV) {
      my $s = 1;
      foreach $type (@cmp) {    # convert hts_engine format
         make_edfile_convert_gv($type);
         shell("$HHEd{'cnv'} -H $clusmmf{'gv'} $gvcnv{$type} $gvdir/gv.list");
         shell("mv $gvdir/trees.$s $gvtrv{$type}");
         shell("mv $gvdir/pdf.$s $gvpdf{$type}");
         $s++;
      }
      if ( $nosilgv && @slnt > 0 ) {    # gv switch
         make_gv_switch();
      }
   }

   # low-pass filter
   make_lpf();
}

# hts_engine (synthesizing waveforms using hts_engine)
if ($ENGIN) {
   print_time("synthesizing waveforms using hts_engine");

   $type = $tknd{'adp'};
   $mllr = $tran{'sat'};
   $mix  = "SAT+${type}_${mllr}${nAdapt}";
   $dir  = "${prjdir}/gen/qst${qnum}/ver${ver}/$mix/hts_engine";
   mkdir ${dir}, 0755;

   # hts_engine command line & options
   # model file & trees
   $hts_engine = "$ENGINE -td $trv{'dur'} -tf $trv{'lf0'} -tm $trv{'mgc'} -tl $trv{'lpf'} -md $pdf{'dur'} -mf $pdf{'lf0'} -mm $pdf{'mgc'} -ml $pdf{'lpf'} ";

   # window coefficients
   $type = 'mgc';
   for ( $d = 1 ; $d <= $nwin{$type} ; $d++ ) {
      $hts_engine .= "-dm $voice/$win{$type}[$d-1] ";
   }
   $type = 'lf0';
   for ( $d = 1 ; $d <= $nwin{$type} ; $d++ ) {
      $hts_engine .= "-df $voice/$win{$type}[$d-1] ";
   }
   $type = 'lpf';
   $d    = 1;
   $hts_engine .= "-dl $voice/$win{$type}[$d-1] ";

   # control parameters (sampling rate, frame shift, frequency warping, etc.)
   $lgopt = "-l" if ($lg);
   $hts_engine .= "-s $sr -p $fs -a $fw -g $gm $lgopt -b " . ( $pf - 1.0 ) . " ";

   # GV pdfs
   if ($useGV) {
      $hts_engine .= "-cm $gvpdf{'mgc'} -cf $gvpdf{'lf0'} ";
      if ( $nosilgv && @slnt > 0 ) {
         $hts_engine .= "-k $voice/gv-switch.inf ";
      }
      if ($cdgv) {
         $hts_engine .= "-em $gvtrv{'mgc'} -ef $gvtrv{'lf0'} ";
      }
      $hts_engine .= "-b 0.0 ";    # turn off postfiltering
   }

   # generate waveform using hts_engine
   open( SCP, $scp{'gen'} ) || die "Cannot open $!";
   while (<SCP>) {
      $lab = $_;
      chomp($lab);
      $base = `basename $lab .lab`;
      chomp($base);

      print "Synthesizing a speech waveform from $lab using hts_engine...";
      shell("$hts_engine -or ${dir}/${base}.raw -ot ${dir}/${base}.trace $lab");
      shell("$SOX -c 1 -s -$SOXOPTION -t raw -r $sr ${dir}/${base}.raw -c 1 -s -$SOXOPTION -t wav -r $sr ${dir}/${base}.wav");
      print "done.\n";
   }
   close(SCP);
}

# sub routines ============================
sub shell($) {
   my ($command) = @_;
   my ($exit);

   $exit = system($command);

   if ( $exit / 256 != 0 ) {
      die "Error in $command\n";
   }
}

sub print_time ($) {
   my ($message) = @_;
   my ($ruler);

   $message .= `date`;

   $ruler = '';
   for ( $i = 0 ; $i <= length($message) + 10 ; $i++ ) {
      $ruler .= '=';
   }

   print "\n$ruler\n";
   print "Start @_ at " . `date`;
   print "$ruler\n\n";
}

# sub routine for generating proto-type model
sub make_proto {
   my ( $i, $j, $k, $s );

   # output prototype definition
   # open proto type definition file
   open( PROTO, ">$prtfile{'cmp'}" ) || die "Cannot open $!";

   # output header
   # output vector size & feature type
   print PROTO "~o <VecSize> $vSize{'cmp'}{'total'} <USER> <DIAGC>";

   # output information about multi-space probability distribution (MSD)
   print PROTO "<MSDInfo> $nstream{'cmp'}{'total'} ";
   foreach $type (@cmp) {
      for ( $s = $strb{$type} ; $s <= $stre{$type} ; $s++ ) {
         print PROTO " $msdi{$type} ";
      }
   }

   # output information about stream
   print PROTO "<StreamInfo> $nstream{'cmp'}{'total'}";
   foreach $type (@cmp) {
      for ( $s = $strb{$type} ; $s <= $stre{$type} ; $s++ ) {
         printf PROTO " %d", $vSize{'cmp'}{$type} / $nstream{'cmp'}{$type};
      }
   }
   print PROTO "\n";

   # output HMMs
   print PROTO "<BeginHMM>\n";
   printf PROTO "  <NumStates> %d\n", $nState + 2;

   # output HMM states
   for ( $i = 2 ; $i <= $nState + 1 ; $i++ ) {

      # output state information
      print PROTO "  <State> $i\n";

      # output stream weight
      print PROTO "  <SWeights> $nstream{'cmp'}{'total'}";
      foreach $type (@cmp) {
         for ( $s = $strb{$type} ; $s <= $stre{$type} ; $s++ ) {
            print PROTO " $strw{$type}";
         }
      }
      print PROTO "\n";

      # output stream information
      foreach $type (@cmp) {
         for ( $s = $strb{$type} ; $s <= $stre{$type} ; $s++ ) {
            print PROTO "  <Stream> $s\n";
            if ( $msdi{$type} == 0 ) {    # non-MSD stream
                                          # output mean vector
               printf PROTO "    <Mean> %d\n", $vSize{'cmp'}{$type} / $nstream{'cmp'}{$type};
               for ( $k = 1 ; $k <= $vSize{'cmp'}{$type} / $nstream{'cmp'}{$type} ; $k++ ) {
                  print PROTO "      " if ( $k % 10 == 1 );
                  print PROTO "0.0 ";
                  print PROTO "\n" if ( $k % 10 == 0 );
               }
               print PROTO "\n" if ( $k % 10 != 1 );

               # output covariance matrix (diag)
               printf PROTO "    <Variance> %d\n", $vSize{'cmp'}{$type} / $nstream{'cmp'}{$type};
               for ( $k = 1 ; $k <= $vSize{'cmp'}{$type} / $nstream{'cmp'}{$type} ; $k++ ) {
                  print PROTO "      " if ( $k % 10 == 1 );
                  print PROTO "1.0 ";
                  print PROTO "\n" if ( $k % 10 == 0 );
               }
               print PROTO "\n" if ( $k % 10 != 1 );
            }
            else {    # MSD stream
                      # output MSD
               print PROTO "  <NumMixes> 2\n";

               # output 1st space (non 0-dimensional space)
               # output space weights
               print PROTO "  <Mixture> 1 0.5000\n";

               # output mean vector
               printf PROTO "    <Mean> %d\n", $vSize{'cmp'}{$type} / $nstream{'cmp'}{$type};
               for ( $k = 1 ; $k <= $vSize{'cmp'}{$type} / $nstream{'cmp'}{$type} ; $k++ ) {
                  print PROTO "      " if ( $k % 10 == 1 );
                  print PROTO "0.0 ";
                  print PROTO "\n" if ( $k % 10 == 0 );
               }
               print PROTO "\n" if ( $k % 10 != 1 );

               # output covariance matrix (diag)
               printf PROTO "    <Variance> %d\n", $vSize{'cmp'}{$type} / $nstream{'cmp'}{$type};
               for ( $k = 1 ; $k <= $vSize{'cmp'}{$type} / $nstream{'cmp'}{$type} ; $k++ ) {
                  print PROTO "      " if ( $k % 10 == 1 );
                  print PROTO "1.0 ";
                  print PROTO "\n" if ( $k % 10 == 0 );
               }
               print PROTO "\n" if ( $k % 10 != 1 );

               # output 2nd space (0-dimensional space)
               print PROTO "  <Mixture> 2 0.5000\n";
               print PROTO "    <Mean> 0\n";
               print PROTO "    <Variance> 0\n";
            }
         }
      }
   }

   # output state transition matrix
   printf PROTO "  <TransP> %d\n", $nState + 2;
   print PROTO "    ";
   for ( $j = 1 ; $j <= $nState + 2 ; $j++ ) {
      print PROTO "1.000e+0 " if ( $j == 2 );
      print PROTO "0.000e+0 " if ( $j != 2 );
   }
   print PROTO "\n";
   print PROTO "    ";
   for ( $i = 2 ; $i <= $nState + 1 ; $i++ ) {
      for ( $j = 1 ; $j <= $nState + 2 ; $j++ ) {
         print PROTO "6.000e-1 " if ( $i == $j );
         print PROTO "4.000e-1 " if ( $i == $j - 1 );
         print PROTO "0.000e+0 " if ( $i != $j && $i != $j - 1 );
      }
      print PROTO "\n";
      print PROTO "    ";
   }
   for ( $j = 1 ; $j <= $nState + 2 ; $j++ ) {
      print PROTO "0.000e+0 ";
   }
   print PROTO "\n";

   # output footer
   print PROTO "<EndHMM>\n";

   close(PROTO);
}

sub make_duration_vfloor {
   my ( $dm, $dv ) = @_;
   my ( $i, $j );

   # output variance flooring macro for duration model
   open( VF, ">$vfloors{'dur'}" ) || die "Cannot open $!";
   for ( $i = 1 ; $i <= $nState ; $i++ ) {
      print VF "~v varFloor$i\n";
      print VF "<Variance> 1\n";
      $j = $dv * $vflr{'dur'};
      print VF " $j\n";
   }
   close(VF);

   # output average model for duration model
   open( MMF, ">$avermmf{'dur'}" ) || die "Cannot open $!";
   print MMF "~o\n";
   print MMF "<STREAMINFO> $nState";
   for ( $i = 1 ; $i <= $nState ; $i++ ) {
      print MMF " 1";
   }
   print MMF "\n";
   print MMF "<VECSIZE> 5<NULLD><USER><DIAGC>\n";
   print MMF "~h \"$avermmf{'dur'}\"\n";
   print MMF "<BEGINHMM>\n";
   print MMF "<NUMSTATES> 3\n";
   print MMF "<STATE> 2\n";
   for ( $i = 1 ; $i <= $nState ; $i++ ) {
      print MMF "<STREAM> $i\n";
      print MMF "<MEAN> 1\n";
      print MMF " $dm\n";
      print MMF "<VARIANCE> 1\n";
      print MMF " $dv\n";
   }
   print MMF "<TRANSP> 3\n";
   print MMF " 0.0 1.0 0.0\n";
   print MMF " 0.0 0.0 1.0\n";
   print MMF " 0.0 0.0 0.0\n";
   print MMF "<ENDHMM>\n";
   close(MMF);
}

# sub routine for generating proto-type model for GV
sub make_proto_gv {
   my ( $s, $type, $k );

   open( PROTO, "> $prtfile{'gv'}" ) || die "Cannot open $!";
   $s = 0;
   foreach $type (@cmp) {
      $s += $ordr{$type};
   }
   print PROTO "~o <VecSize> $s <USER> <DIAGC>\n";
   print PROTO "<MSDInfo> $nPdfStreams{'cmp'} ";
   foreach $type (@cmp) {
      print PROTO "0 ";
   }
   print PROTO "\n";
   print PROTO "<StreamInfo> $nPdfStreams{'cmp'} ";
   foreach $type (@cmp) {
      print PROTO "$ordr{$type} ";
   }
   print PROTO "\n";
   print PROTO "<BeginHMM>\n";
   print PROTO "  <NumStates> 3\n";
   print PROTO "  <State> 2\n";
   $s = 1;
   foreach $type (@cmp) {
      print PROTO "  <Stream> $s\n";
      print PROTO "    <Mean> $ordr{$type}\n";
      for ( $k = 1 ; $k <= $ordr{$type} ; $k++ ) {
         print PROTO "      " if ( $k % 10 == 1 );
         print PROTO "0.0 ";
         print PROTO "\n" if ( $k % 10 == 0 );
      }
      print PROTO "\n" if ( $k % 10 != 1 );
      print PROTO "    <Variance> $ordr{$type}\n";
      for ( $k = 1 ; $k <= $ordr{$type} ; $k++ ) {
         print PROTO "      " if ( $k % 10 == 1 );
         print PROTO "1.0 ";
         print PROTO "\n" if ( $k % 10 == 0 );
      }
      print PROTO "\n" if ( $k % 10 != 1 );
      $s++;
   }
   print PROTO "  <TransP> 3\n";
   print PROTO "    0.000e+0 1.000e+0 0.000e+0 \n";
   print PROTO "    0.000e+0 0.000e+0 1.000e+0 \n";
   print PROTO "    0.000e+0 0.000e+0 0.000e+0 \n";
   print PROTO "<EndHMM>\n";
   close(PROTO);
}

# sub routine for making training data, labels, scp, list, and mlf for GV
sub make_data_gv {
   my ( $type, $cmp, $base, $str, @arr, $start, $end, $find, $i, $j );

   foreach $type (@cmp) {
      shell("rm -f $scp{'gv'}");
      shell("touch $scp{'gv'}");
   }
   open( SCP, $scp{'trn'} ) || die "Cannot open $!";
   if ($cdgv) {
      open( LST, "> $gvdir/tmp.list" );
   }
   while (<SCP>) {
      $cmp = $_;
      chomp($cmp);
      $sbase = `dirname $cmp`;
      chomp($sbase);
      $sbase = `basename $sbase`;
      chomp($sbase);
      $base = `basename $cmp .cmp`;
      chomp($base);
      print "Making data, labels, and scp from $base.lab for GV...";
      shell("rm -f $gvdatdir/tmp.cmp");
      shell("touch $gvdatdir/tmp.cmp");
      $i = 0;

      foreach $type (@cmp) {
         if ( $nosilgv && @slnt > 0 ) {
            shell("rm -f $gvdatdir/tmp.$type");
            shell("touch $gvdatdir/tmp.$type");
            open( F, "$gvfaldir/$base.lab" ) || die "Cannot open $!";
            while ( $str = <F> ) {
               chomp($str);
               @arr = split( / /, $str );
               $find = 0;
               for ( $j = 0 ; $j < @slnt ; $j++ ) {
                  if ( $arr[2] eq "$slnt[$j]" ) { $find = 1; last; }
               }
               if ( $find == 0 ) {
                  $start = int( $arr[0] * ( 1.0e-7 / ( $fs / $sr ) ) );
                  $end   = int( $arr[1] * ( 1.0e-7 / ( $fs / $sr ) ) );
                  shell("$BCUT -s $start -e $end -l $ordr{$type} +f $datdir/$type/$sbase/$base.$type >> $gvdatdir/tmp.$type");
               }
            }
            close(F);
         }
         else {
            shell("cp $datdir/$type/$sbase/$base.$type $gvdatdir/tmp.$type");
         }
         if ( $msdi{$type} == 0 ) {
            shell("cat      $gvdatdir/tmp.$type                              | $VSTAT -d -l $ordr{$type} -o 2 >> $gvdatdir/tmp.cmp");
         }
         else {
            shell("$X2X +fa $gvdatdir/tmp.$type | grep -v '1e+10' | $X2X +af | $VSTAT -d -l $ordr{$type} -o 2 >> $gvdatdir/tmp.cmp");
         }
         system("rm -f $gvdatdir/tmp.$type");
         $i += 4 * $ordr{$type};
      }
      shell("$PERL $datdir/scripts/addhtkheader.pl $sr $fs $i 9 $gvdatdir/tmp.cmp > $gvdatdir/$base.cmp");
      $i = `$NAN $gvdatdir/$base.cmp`;
      chomp($i);
      if ( length($i) > 0 ) {
         shell("rm -f $gvdatdir/$base.cmp");
      }
      else {
         shell("echo $gvdatdir/$base.cmp >> $scp{'gv'}");
         if ($cdgv) {
            open( LAB, "$datdir/labels/full/$sbase/$base.lab" ) || die "Cannot open $!";
            $str = <LAB>;
            close(LAB);
            chomp($str);
            while ( index( $str, " " ) >= 0 || index( $str, "\t" ) >= 0 ) { substr( $str, 0, 1 ) = ""; }
            open( LAB, "> $gvlabdir/$base.lab" ) || die "Cannot open $!";
            print LAB "$str\n";
            close(LAB);
            print LST "$str\n";
         }
      }
      system("rm -f $gvdatdir/tmp.cmp");
      print "done\n";
   }
   if ($cdgv) {
      close(LST);
      system("sort -u $gvdir/tmp.list > $gvdir/gv.list");
      system("rm -f $gvdir/tmp.list");
   }
   else {
      system("echo gv > $gvdir/gv.list");
   }
   close(SCP);

   # make mlf
   open( MLF, "> $mlf{'gv'}" ) || die "Cannot open $!";
   print MLF "#!MLF!#\n";
   print MLF "\"*/*.lab\" -> \"$gvlabdir\"\n";
   close(MLF);
}

# sub routine to copy average.mmf to full.mmf for GV
sub copy_aver2full_gv {
   my ( $find, $head, $tail, $str );

   $find = 0;
   $head = "";
   $tail = "";
   open( MMF, "$avermmf{'gv'}" ) || die "Cannot open $!";
   while ( $str = <MMF> ) {
      if ( index( $str, "~h" ) >= 0 ) {
         $find = 1;
      }
      elsif ( $find == 0 ) {
         $head .= $str;
      }
      else {
         $tail .= $str;
      }
   }
   close(MMF);
   $head .= `cat $gvdir/vFloors`;
   open( LST, "$gvdir/gv.list" )   || die "Cannot open $!";
   open( MMF, "> $fullmmf{'gv'}" ) || die "Cannot open $!";
   print MMF "$head";
   while ( $str = <LST> ) {
      chomp($str);
      print MMF "~h \"$str\"\n";
      print MMF "$tail";
   }
   close(MMF);
   close(LST);
}

sub copy_aver2clus_gv {
   my ( $find, $head, $mid, $tail, $str, $tmp, $s, @pdfs );

   # initaialize
   $find = 0;
   $head = "";
   $mid  = "";
   $tail = "";
   $s    = 0;
   @pdfs = ();
   foreach $type (@cmp) {
      push( @pdfs, "" );
   }

   # load
   open( MMF, "$avermmf{'gv'}" ) || die "Cannot open $!";
   while ( $str = <MMF> ) {
      if ( index( $str, "~h" ) >= 0 ) {
         $head .= `cat $gvdir/vFloors`;
         last;
      }
      else {
         $head .= $str;
      }
   }
   while ( $str = <MMF> ) {
      if ( index( $str, "<STREAM>" ) >= 0 ) {
         last;
      }
      else {
         $mid .= $str;
      }
   }
   while ( $str = <MMF> ) {
      if ( index( $str, "<TRANSP>" ) >= 0 ) {
         $tail .= $str;
         last;
      }
      elsif ( index( $str, "<STREAM>" ) >= 0 ) {
         $s++;
      }
      else {
         $pdfs[$s] .= $str;
      }
   }
   while ( $str = <MMF> ) {
      $tail .= $str;
   }
   close(MMF);

   # save
   open( MMF, "> $clusmmf{'gv'}" ) || die "Cannot open $!";
   print MMF "$head";
   $s = 1;
   foreach $type (@cmp) {
      print MMF "~p \"gv_${type}_1\"\n";
      print MMF "<STREAM> $s\n";
      print MMF "$pdfs[$s-1]";
      $s++;
   }
   print MMF "~h \"gv\"\n";
   print MMF "$mid";
   $s = 1;
   foreach $type (@cmp) {
      print MMF "<STREAM> $s\n";
      print MMF "~p \"gv_${type}_1\"\n";
      $s++;
   }
   print MMF "$tail";
   close(MMF);
   close(LST);
}

sub copy_clus2clsa_gv {
   shell("cp $clusmmf{'gv'} $clsammf{'gv'}");
   shell("cp $gvdir/gv.list $tiedlst{'gv'}");
}

# sub routine for generating config files
sub make_config() {
   my ( $s, $type, @boolstring );
   $boolstring[0] = 'FALSE';
   $boolstring[1] = 'TRUE';

   # config file for model training
   open( CONF, ">$cfg{'trn'}" ) || die "Cannot open $!";
   print CONF "APPLYVFLOOR = T\n";
   print CONF "NATURALREADORDER = T\n";
   print CONF "NATURALWRITEORDER = T\n";
   print CONF "VFLOORSCALESTR = \"Vector $nstream{'cmp'}{'total'}";
   foreach $type (@cmp) {
      for ( $s = $strb{$type} ; $s <= $stre{$type} ; $s++ ) {
         print CONF " $vflr{$type}";
      }
   }
   print CONF "\"\n";
   printf CONF "DURVARFLOORPERCENTILE = %f\n", 100 * $vflr{'dur'};
   print CONF "APPLYDURVARFLOOR = T\n";
   print CONF "MAXSTDDEVCOEF = $maxdev\n";
   print CONF "MINDUR = $mindur\n";
   close(CONF);

   # config file for model training (without variance flooring)
   open( CONF, ">$cfg{'nvf'}" ) || die "Cannot open $!";
   print CONF "APPLYVFLOOR = F\n";
   print CONF "DURVARFLOORPERCENTILE = 0.0\n";
   print CONF "APPLYDURVARFLOOR = T\n";
   close(CONF);

   # config file for model tying
   foreach $type (@cmp) {
      open( CONF, ">$cfg{$type}" ) || die "Cannot open $!";
      print CONF "MINLEAFOCC = $mocc{$type}\n";
      close(CONF);
   }
   foreach $type (@dur) {
      open( CONF, ">$cfg{$type}" ) || die "Cannot open $!";
      print CONF "MINLEAFOCC = $mocc{$type}\n";
      close(CONF);
   }

   # config file for building regression tree
   foreach $set (@SET) {
      open( CONF, ">$cfg{'dec'}{$set}" ) || die "Cannot open $!";
      print CONF "SHRINKOCCTHRESH = \"Vector $nstream{$set}{'total'}";
      foreach $type ( @{ $ref{$set} } ) {
         for ( $s = $strb{$type} ; $s <= $stre{$type} ; $s++ ) {
            print CONF " $adpt{$type}";
         }
      }
      print CONF "\"\n";
      close(CONF);
   }

   # config file for model conversion
   open( CONF, ">$cfg{'cnv'}" ) || die "Cannot open $!";
   print CONF "NATURALREADORDER = T\n";
   print CONF "NATURALWRITEORDER = F\n";    # hts_engine used BIG ENDIAN
   close(CONF);

   # config file for parameter generation
   open( CONF, ">$cfg{'syn'}" ) || die "Cannot open $!";
   print CONF "NATURALREADORDER = T\n";
   print CONF "NATURALWRITEORDER = T\n";
   print CONF "USEALIGN = T\n";

   print CONF "PDFSTRSIZE = \"IntVec $nPdfStreams{'cmp'}";    # PdfStream structure
   foreach $type (@cmp) {
      print CONF " $nstream{'cmp'}{$type}";
   }
   print CONF "\"\n";

   print CONF "PDFSTRORDER = \"IntVec $nPdfStreams{'cmp'}";    # order of each PdfStream
   foreach $type (@cmp) {
      print CONF " $ordr{$type}";
   }
   print CONF "\"\n";

   print CONF "PDFSTREXT = \"StrVec $nPdfStreams{'cmp'}";      # filename extension for each PdfStream
   foreach $type (@cmp) {
      print CONF " $type";
   }
   print CONF "\"\n";

   print CONF "WINFN = \"";
   foreach $type (@cmp) {
      print CONF "StrVec $nwin{$type} @{$win{$type}} ";        # window coefficients files for each PdfStream
   }
   print CONF "\"\n";
   print CONF "WINDIR = $windir\n";                            # directory which stores window coefficients files

   print CONF "MAXEMITER = $maxEMiter\n";
   print CONF "EMEPSILON = $EMepsilon\n";
   print CONF "USEGV      = $boolstring[$useGV]\n";
   print CONF "GVMODELMMF = $clsammf{'gv'}\n";
   print CONF "GVHMMLIST  = $tiedlst{'gv'}\n";
   print CONF "MAXGVITER  = $maxGViter\n";
   print CONF "GVEPSILON  = $GVepsilon\n";
   print CONF "MINEUCNORM = $minEucNorm\n";
   print CONF "STEPINIT   = $stepInit\n";
   print CONF "STEPINC    = $stepInc\n";
   print CONF "STEPDEC    = $stepDec\n";
   print CONF "HMMWEIGHT  = $hmmWeight\n";
   print CONF "GVWEIGHT   = $gvWeight\n";
   print CONF "OPTKIND    = $optKind\n";

   if ( $nosilgv && @slnt > 0 ) {
      $s = @slnt;
      print CONF "GVOFFMODEL = \"StrVec $s";
      for ( $s = 0 ; $s < @slnt ; $s++ ) {
         print CONF " $slnt[$s]";
      }
      print CONF "\"\n";
   }
   print CONF "CDGV       = $boolstring[$cdgv]\n";

   close(CONF);
}

# sub routine for generating config files for adaptation
sub make_config_adapt($$) {
   my ( $regtype, $mllr ) = @_;
   my ( $s, $b, $type, $bSize );

   # config file for adaptation
   if ( $mllr eq 'sat' ) {
      open( CONF, ">$cfg{'sat'}" ) || die "Cannot open $!";
      print CONF "ADAPTKIND    = $aknd{'sat'}\n";
      print CONF "DURADAPTKIND = $aknd{'sat'}\n";
   }
   else {
      open( CONF, ">$cfg{'adp'}" ) || die "Cannot open $!";
      print CONF "MAXSTDDEVCOEF = 10\n";    # expand max # of state durations
      print CONF "ADAPTKIND    = $aknd{'adp'}\n";
      print CONF "DURADAPTKIND = $aknd{'adp'}\n";
   }

   print CONF "BASECLASS      = \"$rbase{'cmp'}{$regtype}\"\n";
   print CONF "REGTREE        = \"$rtree{'cmp'}{$regtype}\"\n";
   print CONF "DURBASECLASS   = \"$rbase{'dur'}{$regtype}\"\n";
   print CONF "DURREGTREE     = \"$rtree{'dur'}{$regtype}\"\n";

   # adaptation transform kind
   if ( $mllr eq "mean" ) {
      print CONF "TRANSKIND    = MLLRMEAN\n";    # mean transform
      print CONF "USEBIAS      = $bias{'cmp'}\n";
      print CONF "DURTRANSKIND = MLLRMEAN\n";
      print CONF "DURUSEBIAS   = $bias{'dur'}\n";
      print CONF "MLLRDIAGCOV  = $dcov\n";
      print CONF "USESMAP      = $smap\n";
      print CONF "SMAPSIGMA    = $sigma\n";
   }
   elsif ( $mllr eq "cov" ) {
      print CONF "TRANSKIND    = MLLRCOV\n";     # covariance transform
      print CONF "USEBIAS      = FALSE\n";
      print CONF "DURTRANSKIND = MLLRCOV\n";
      print CONF "DURUSEBIAS   = FALSE\n";
      print CONF "MLLRDIAGCOV  = FALSE\n";
      print CONF "USESMAP      = FALSE\n";
   }
   elsif ( $mllr eq "feat" ) {
      print CONF "TRANSKIND    = CMLLR\n";       # feature transform
      print CONF "USEBIAS      = $bias{'cmp'}\n";
      print CONF "DURTRANSKIND = CMLLR\n";
      print CONF "DURUSEBIAS   = $bias{'dur'}\n";
      print CONF "MLLRDIAGCOV  = FALSE\n";
      print CONF "USESMAP      = $smap\n";
      print CONF "SMAPSIGMA    = $sigma\n";
   }
   elsif ( $mllr eq "sat" ) {
      print CONF "TRANSKIND    = CMLLR\n";       # feature transform
      print CONF "USEBIAS      = $bias{'cmp'}\n";
      print CONF "DURTRANSKIND = CMLLR\n";
      print CONF "DURUSEBIAS   = $bias{'dur'}\n";
      print CONF "MLLRDIAGCOV  = FALSE\n";
      print CONF "USESMAP      = FALSE\n";
   }
   else {
      die "MLLR type $mllr is not supported!";
   }

   # split threshold for adaptation
   # HMM
   print CONF "SPLITTHRESH = \"Vector $nstream{'cmp'}{'total'}";
   foreach $type (@cmp) {
      for ( $s = $strb{$type} ; $s <= $stre{$type} ; $s++ ) {
         if ( $mllr eq "sat" ) {
            print CONF " $satt{$type}";
         }
         else {
            print CONF " $adpt{$type}";
         }
      }
   }
   print CONF "\"\n";

   # duration model
   print CONF "DURSPLITTHRESH = \"Vector $nState";
   foreach $type (@dur) {
      for ( $s = $strb{$type} ; $s <= $stre{$type} ; $s++ ) {
         if ( $mllr eq "sat" ) {
            print CONF " $satt{$type}";
         }
         else {
            print CONF " $adpt{$type}";
         }
      }
   }
   print CONF "\"\n";

   # block size
   print CONF "BLOCKSIZE   = \"";
   foreach $type (@cmp) {
      for ( $s = $strb{$type} ; $s <= $stre{$type} ; $s++ ) {
         $bSize = $vSize{'cmp'}{$type} / $nstream{'cmp'}{$type} / $nblk{$type};
         print CONF "IntVec $nblk{$type} ";
         for ( $b = 1 ; $b <= $nblk{$type} ; $b++ ) {
            print CONF "$bSize ";
         }
      }
   }
   print CONF "\"\n";

   # band width
   print CONF "BANDWIDTH   = \"";
   foreach $type (@cmp) {
      for ( $s = $strb{$type} ; $s <= $stre{$type} ; $s++ ) {
         $bSize = $vSize{'cmp'}{$type} / $nstream{'cmp'}{$type} / $nblk{$type};
         print CONF "IntVec $nblk{$type} ";
         for ( $b = 1 ; $b <= $nblk{$type} ; $b++ ) {
            print CONF "$band{$type} ";
         }
      }
   }
   print CONF "\"\n";
   close(CONF);
}

sub make_config_map() {

   # config file for MAP adaptation
   open( CONF, ">$cfg{'map'}" ) || die "Cannot open $!";
   print CONF "MAPTAU         = $maptau{'cmp'}\n";
   print CONF "DURMAPTAU      = $maptau{'dur'}\n";
   print CONF "MINEGS         = 1\n";
   print CONF "APPLYVFLOOR    = T\n";
   print CONF "MIXWEIGHTFLOOR = $wf\n";
   print CONF "HMAP:TRACE     = 02\n";
   close(CONF);
}

sub make_config_align() {

   # config file for alignment model
   open( CONF, ">$cfg{'aln'}" ) || die "Cannot open $!";
   print CONF "ALIGNMODELMMF = $rclammf{'cmp'}\n";
   print CONF "ALIGNDURMMF   = $rclammf{'dur'}\n";
   print CONF "ALIGNHMMLIST  = $tiedlst{'cmp'}\n";
   print CONF "ALIGNDURLIST  = $tiedlst{'dur'}\n";
   close(CONF);
}

# sub routine for generating .hed files for decision-tree clustering
sub make_edfile_state($) {
   my ($type) = @_;
   my ( @lines, $i, @nstate );

   $nstate{'cmp'} = $nState;
   $nstate{'dur'} = 1;

   open( QSFILE, "$qs{$type}" ) || die "Cannot open $!";
   @lines = <QSFILE>;
   close(QSFILE);

   open( EDFILE, ">$cxc{$type}" ) || die "Cannot open $!";
   print EDFILE "// load stats file\n";
   print EDFILE "RO $gam{$type} \"$stats{$t2s{$type}}\"\n\n";
   print EDFILE "TR 0\n\n";
   print EDFILE "// questions for decision tree-based context clustering\n";
   print EDFILE @lines;
   print EDFILE "TR 3\n\n";
   print EDFILE "// construct decision trees\n";

   for ( $i = 2 ; $i <= $nstate{ $t2s{$type} } + 1 ; $i++ ) {
      print EDFILE "TB $thr{$type} ${type}_s${i}_ {*.state[${i}].stream[$strb{$type}-$stre{$type}]}\n";
   }
   print EDFILE "\nTR 1\n\n";
   print EDFILE "// output constructed trees\n";
   print EDFILE "ST \"$tre{$type}\"\n";
   close(EDFILE);
}

# sub routine for generating .hed files for decision-tree clustering of GV
sub make_edfile_state_gv($$) {
   my ( $type, $s ) = @_;
   my (@lines);

   open( QSFILE, "$qs_utt{$type}" ) || die "Cannot open $!";
   @lines = <QSFILE>;
   close(QSFILE);

   open( EDFILE, ">$gvcxc{$type}" ) || die "Cannot open $!";
   if ($cdgv) {
      print EDFILE "// load stats file\n";
      print EDFILE "RO $gvgam{$type} \"$gvdir/gv.stats\"\n";
      print EDFILE "TR 0\n\n";
      print EDFILE "// questions for decision tree-based context clustering\n";
      print EDFILE @lines;
      print EDFILE "TR 3\n\n";
      print EDFILE "// construct decision trees\n";
      print EDFILE "TB $gvthr{$type} gv_${type}_ {*.state[2].stream[$s]}\n";
      print EDFILE "\nTR 1\n\n";
      print EDFILE "// output constructed trees\n";
      print EDFILE "ST \"$gvtre{$type}\"\n";
   }
   else {
      open( TREE, ">$gvtre{$type}" ) || die "Cannot open $!";
      print TREE " {*}[2].stream[$s]\n   \"gv_${type}_1\"\n";
      close(TREE);
      print EDFILE "// construct tying structure\n";
      print EDFILE "TI gv_${type}_1 {*.state[2].stream[$s]}\n";
   }
   close(EDFILE);
}

# sub routine for untying structures
sub make_edfile_untie($) {
   my ($set) = @_;
   my ( $type, $i, @nstate );

   $nstate{'cmp'} = $nState;
   $nstate{'dur'} = 1;

   open( EDFILE, ">$unt{$set}" ) || die "Cannot open $!";

   print EDFILE "// untie parameter sharing structure\n";
   foreach $type ( @{ $ref{$set} } ) {
      for ( $i = 2 ; $i <= $nstate{$set} + 1 ; $i++ ) {
         if ( $#{ $ref{$set} } eq 0 ) {
            print EDFILE "UT {*.state[$i]}\n";
         }
         else {
            if ( $strw{$type} > 0.0 ) {
               print EDFILE "UT {*.state[$i].stream[$strb{$type}-$stre{$type}]}\n";
            }
         }
      }
   }

   close(EDFILE);
}

# sub routine to increase the number of mixture components
sub make_edfile_upmix($) {
   my ($set) = @_;
   my ( $type, $i, @nstate );

   $nstate{'cmp'} = $nState;
   $nstate{'dur'} = 1;

   open( EDFILE, ">$upm{$set}" ) || die "Cannot open $!";

   print EDFILE "// increase the number of mixtures per stream\n";
   foreach $type ( @{ $ref{$set} } ) {
      for ( $i = 2 ; $i <= $nstate{$set} + 1 ; $i++ ) {
         if ( $#{ $ref{$set} } eq 0 ) {
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
   open( IN,  "$stats{'cmp'}" )  || die "Cannot open $!";
   open( OUT, ">$stats{'dur'}" ) || die "Cannot open $!";
   while (<IN>) {
      @LINE = split(' ');
      printf OUT ( "%4d %14s %4d %4d\n", $LINE[0], $LINE[1], $LINE[2], $LINE[2] );
   }
   close(IN);
   close(OUT);
}

# sub routine for generating .hed files for mmf -> hts_engine conversion
sub make_edfile_convert($$) {
   my ( $type, $spxf ) = @_;

   open( EDFILE, ">$cnv{$type}" ) || die "Cannot open $!";
   print EDFILE "\nTR 2\n\n";

   print EDFILE "// load adaptation transforms\n";
   print EDFILE "AX \"$spxf\"\n";

   print EDFILE "// load trees for $type\n";
   print EDFILE "LT \"$tre{$type}\"\n\n";

   print EDFILE "// convert loaded trees for hts_engine format\n";
   print EDFILE "CT \"$trd{$t2s{$type}}\"\n\n";

   print EDFILE "// convert mmf for hts_engine format\n";
   print EDFILE "CM \"$model{$t2s{$type}}\"\n";

   close(EDFILE);
}

# sub routine for generating .hed files for GV mmf -> hts_engine conversion
sub make_edfile_convert_gv($) {
   my ($type) = @_;

   open( EDFILE, ">$gvcnv{$type}" ) || die "Cannot open $!";
   print EDFILE "\nTR 2\n\n";
   print EDFILE "// load trees for $type\n";
   print EDFILE "LT \"$gvdir/$type.inf\"\n\n";

   print EDFILE "// convert loaded trees for hts_engine format\n";
   print EDFILE "CT \"$gvdir\"\n\n";

   print EDFILE "// convert mmf for hts_engine format\n";
   print EDFILE "CM \"$gvdir\"\n";

   close(EDFILE);
}

# sub routine for generating .hed files for making unseen models
sub make_edfile_mkunseen($) {
   my ($set) = @_;
   my ($type);

   open( EDFILE, ">$mku{$set}" ) || die "Cannot open $!";
   print EDFILE "\nTR 2\n\n";
   foreach $type ( @{ $ref{$set} } ) {
      print EDFILE "// load trees for $type\n";
      print EDFILE "LT \"$tre{$type}\"\n\n";
   }

   print EDFILE "// make unseen model\n";
   print EDFILE "AU \"$lst{'all'}\"\n\n";
   print EDFILE "// make model compact\n";
   print EDFILE "CO \"$tiedlst{$set}\"\n\n";

   close(EDFILE);
}

# sub routine for generating .hed files for constructing a regression tree
sub make_edfile_regtree($$) {
   my ( $regtype, $set ) = @_;
   my ($type);

   open( EDFILE, ">$red{$set}{$regtype}" ) || die "Cannot open $!";
   if ( $regtype eq 'reg' ) {
      print EDFILE "// load stats file\n";
      print EDFILE "LS \"$stats{$set}\"\n";
      print EDFILE "// construct regression class tree\n";
      print EDFILE "RC $nClass \"$regtype\"\n";
   }
   else {
      print EDFILE "// load stats file\n";
      print EDFILE "LS \"$stats{$set}\"\n";
      foreach $type ( @{ $ref{$set} } ) {
         print EDFILE "// load trees for $type\n";
         print EDFILE "LT \"$tre{$type}\"\n\n";
      }
      print EDFILE "// convert decision trees to a regression class tree\n";
      print EDFILE "DR \"$regtype\"\n";
   }
   close(EDFILE);
}

# sub routine for generating .hed files for making unseen models for GV
sub make_edfile_mkunseen_gv() {
   my ($type);

   open( EDFILE, ">$mku{'gv'}" ) || die "Cannot open $!";
   print EDFILE "\nTR 2\n\n";
   foreach $type (@cmp) {
      print EDFILE "// load trees for $type\n";
      print EDFILE "LT \"$gvtre{$type}\"\n\n";
   }

   print EDFILE "// make unseen model\n";
   print EDFILE "AU \"$lst{'all'}\"\n\n";
   print EDFILE "// make model compact\n";
   print EDFILE "CO \"$tiedlst{'gv'}\"\n\n";

   close(EDFILE);
}

sub make_gv_switch() {
   my ($i);

   open( SWITCH, "> $voice/gv-switch.inf" ) || die "Cannot open $!";
   print SWITCH "QS gv-switch { ";
   for ( $i = 0 ; $i < @slnt ; $i++ ) {
      if ( $i > 0 ) {
         print SWITCH ",";
      }
      print SWITCH "\"*-$slnt[$i]+*\"";
   }
   print SWITCH " }\n";
   print SWITCH "{*}[2]\n";
   print SWITCH "{\n";
   print SWITCH "   0 gv-switch \"gv-switch_2\" \"gv-switch_1\"\n";
   print SWITCH "}\n";
   close(SWITCH);
}

sub make_lpf() {
   my ( $lfil, @coef, $coefSize, $i, $j );

   $lfil     = `$PERL $datdir/scripts/makefilter.pl $sr 0`;
   @coef     = split( / /, $lfil );
   $coefSize = @coef;
   shell("echo 0 | $X2X +ai | $SWAB +f >  $pdf{'lpf'}");
   shell("echo 1 | $X2X +ai | $SWAB +f >> $pdf{'lpf'}");
   shell("echo $coefSize | $X2X +ai | $SWAB +f >> $pdf{'lpf'}");
   for ( $i = 0 ; $i < $nState ; $i++ ) {
      shell("echo 1 | $X2X +ai | $SWAB +f >> $pdf{'lpf'}");
   }
   for ( $i = 0 ; $i < $nState ; $i++ ) {
      for ( $j = 0 ; $j < $coefSize ; $j++ ) {
         shell("echo $coef[$j] 0.0 | $X2X +af | $SWAB +f >> $pdf{'lpf'}");
      }
   }

   open( INF, "> $trv{'lpf'}" );
   for ( $i = 2 ; $i <= $nState + 1 ; $i++ ) {
      print INF "{*}[${i}]\n";
      print INF "   \"lpf_s${i}_1\"\n";
   }
   close(INF);

   open( WIN, "> $voice/lpf.win1" );
   print WIN "1 1.0\n";
   close(WIN);
}

# sub routine for log f0 -> f0 conversion
sub lf02pitch($$) {
   my ( $base, $gendir ) = @_;
   my ( $t, $T, $data );

   # read log f0 file
   open( IN, "$gendir/${base}.lf0" );
   @STAT = stat(IN);
   read( IN, $data, $STAT[7] );
   close(IN);

   # log f0 -> pitch conversion
   $T = $STAT[7] / 4;
   @frq = unpack( "f$T", $data );
   for ( $t = 0 ; $t < $T ; $t++ ) {
      if ( $frq[$t] == -1.0e+10 ) {
         $out[$t] = 0.0;
      }
      else {
         $out[$t] = $sr / exp( $frq[$t] );
      }
   }
   $data = pack( "f$T", @out );

   # output data
   open( OUT, ">$gendir/${base}.pit" );
   print OUT $data;
   close(OUT);
}

# sub routine for formant emphasis in Mel-cepstral domain
sub postfiltering($$) {
   my ( $base, $gendir ) = @_;
   my ( $i, $line );

   # output postfiltering weight coefficient
   $line = "echo 1 1 ";
   for ( $i = 2 ; $i < $ordr{'mgc'} ; $i++ ) {
      $line .= "$pf ";
   }
   $line .= "| $X2X +af > $gendir/weight";
   shell($line);

   # calculate auto-correlation of original mcep
   $line = "$FREQT -m " . ( $ordr{'mgc'} - 1 ) . " -a $fw -M $co -A 0 < $gendir/${base}.mgc | ";
   $line .= "$C2ACR -m $co -M 0 -l $fl > $gendir/${base}.r0";
   shell($line);

   # calculate auto-correlation of postfiltered mcep
   $line = "$VOPR -m -n " . ( $ordr{'mgc'} - 1 ) . " < $gendir/${base}.mgc $gendir/weight | ";
   $line .= "$FREQT -m " . ( $ordr{'mgc'} - 1 ) . " -a $fw -M $co -A 0 | ";
   $line .= "$C2ACR -m $co -M 0 -l $fl > $gendir/${base}.p_r0";
   shell($line);

   # calculate MLSA coefficients from postfiltered mcep
   $line = "$VOPR -m -n " . ( $ordr{'mgc'} - 1 ) . " < $gendir/${base}.mgc $gendir/weight | ";
   $line .= "$MC2B -m " . ( $ordr{'mgc'} - 1 ) . " -a $fw | ";
   $line .= "$BCP -n " .  ( $ordr{'mgc'} - 1 ) . " -s 0 -e 0 > $gendir/${base}.b0";
   shell($line);

   # calculate 0.5 * log(acr_orig/acr_post)) and add it to 0th MLSA coefficient
   $line = "$VOPR -d < $gendir/${base}.r0 $gendir/${base}.p_r0 | ";
   $line .= "$SOPR -LN -d 2 | ";
   $line .= "$VOPR -a $gendir/${base}.b0 > $gendir/${base}.p_b0";
   shell($line);

   # generate postfiltered mcep
   $line = "$VOPR -m -n " . ( $ordr{'mgc'} - 1 ) . " < $gendir/${base}.mgc $gendir/weight | ";
   $line .= "$MC2B -m " .  ( $ordr{'mgc'} - 1 ) . " -a $fw | ";
   $line .= "$BCP -n " .   ( $ordr{'mgc'} - 1 ) . " -s 1 -e " . ( $ordr{'mgc'} - 1 ) . " | ";
   $line .= "$MERGE -n " . ( $ordr{'mgc'} - 2 ) . " -s 0 -N 0 $gendir/${base}.p_b0 | ";
   $line .= "$B2MC -m " .  ( $ordr{'mgc'} - 1 ) . " -a $fw > $gendir/${base}.p_mgc";
   shell($line);
}

# sub routine for speech synthesis from log f0 and Mel-cepstral coefficients
sub gen_wave($) {
   my ($gendir) = @_;
   my ( $line, @FILE, $file, $base );

   $line  = `ls $gendir/*.mgc`;
   @FILE  = split( '\n', $line );
   $lgopt = "-l" if ($lg);

   print "Processing directory $gendir:\n";
   foreach $file (@FILE) {
      $base = `basename $file .mgc`;
      chomp($base);
      if ( -s $file && -s "$gendir/$base.lf0" ) {
         print " Synthesizing a speech waveform from $base.mgc and $base.lf0...";

         # convert log F0 to pitch
         lf02pitch( $base, $gendir );

         if ( $gm > 0 ) {

            # MGC-LSPs -> MGC coefficients
            $line = "$LSPCHECK -m " . ( $ordr{'mgc'} - 1 ) . " -s " . ( $sr / 1000 ) . " -r 0.1 $file | ";
            $line .= "$LSP2LPC -m " . ( $ordr{'mgc'} - 1 ) . " -s " . ( $sr / 1000 ) . " $lgopt | ";
            $line .= "$MGC2MGC -m " . ( $ordr{'mgc'} - 1 ) . " -a $fw -c $gm -n -u -M " . ( $ordr{'mgc'} - 1 ) . " -A $fw -C $gm " . " > $gendir/$base.c_mgc";
            shell($line);

            $mgc = "$gendir/$base.c_mgc";
         }
         else {

            # apply postfiltering
            if ( $gm == 0 && $pf != 1.0 && $useGV == 0 ) {
               postfiltering( $base, $gendir );
               $mgc = "$gendir/$base.p_mgc";
            }
            else {
               $mgc = $file;
            }
         }

         # synthesize waveform
         $lfil = `$PERL $datdir/scripts/makefilter.pl $sr 0`;
         $hfil = `$PERL $datdir/scripts/makefilter.pl $sr 1`;
         $line = "$SOPR -m 0 $gendir/$base.pit | $EXCITE -p $fs | $DFS -b $hfil > $gendir/$base.unv";
         shell($line);

         $line = "$EXCITE -p $fs $gendir/$base.pit | ";
         $line .= "$DFS -b $lfil | $VOPR -a $gendir/$base.unv | ";
         $line .= "$MGLSADF -m " . ( $ordr{'mgc'} - 1 ) . " -p $fs -a $fw -c $gm $mgc | ";
         $line .= "$X2X +fs -o | ";
         $line .= "$SOX -c 1 -s -$SOXOPTION -t raw -r $sr - -c 1 -s -$SOXOPTION -t wav -r $sr $gendir/$base.wav";
         shell($line);

         $line = "rm -f $gendir/$base.unv";
         shell($line);

         print "done\n";
      }
   }
   print "done\n";
}

##################################################################################################

