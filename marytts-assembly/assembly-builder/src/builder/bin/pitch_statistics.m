#! /bin/octave -qf

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
% pitch_statistics.sh
% Author: Fabio Tesser
% Email: fabio.tesser@gmail.com
%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

printf ("%s\n", program_name ());
arg_list = argv ();

filename=arg_list{1};
epsfilename=sprintf("%s_pitch_statistics.eps",filename);

f0 = load(filename);
hist(f0,200);
print("-depsc",epsfilename);

printf("The statistics of f0\nmean: %f, std %f, min %f, max %f\n",mean(f0),std(f0),min(f0),max(f0));

delta=0.5;
P=[percentile(f0,delta) percentile(f0,100-delta)];
printf("The percentiles at %f %s and %f %s are: [%f - %f]\n", delta,"%", 100-delta, "%",P(1),P(2));

exit;

