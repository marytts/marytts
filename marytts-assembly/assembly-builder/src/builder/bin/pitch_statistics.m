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

[DIR, NAME, EXT, VER] = fileparts (filename);
[STATUS, RESULT, MSGID] = fileattrib (DIR);
[DIR, NAME, EXT, VER] = fileparts (RESULT.Name);
[DIR, NAME, EXT, VER] = fileparts (DIR);

NAME

f0 = load(filename);
printf("The statistics of f0\nmean: %f, std %f, min %f, max %f\n",mean(f0),std(f0),min(f0),max(f0));

delta1=0.1;
P1=[percentile(f0,delta1) percentile(f0,100-delta1)];
printf("The percentiles at %f %s and %f %s are: [%f - %f]\n", delta1,"%", 100-delta1, "%",P1(1),P1(2));

delta2=0.5;
P2=[percentile(f0,delta2) percentile(f0,100-delta2)];
printf("The percentiles at %f %s and %f %s are: [%f - %f]\n", delta2,"%", 100-delta2, "%",P2(1),P2(2));

figure;
[NN, XX]=hist(f0,200);
bar(XX,NN);

%mean(XX)
%mean(NN)

%figure;
%hist(f0,200);
grid on;
title (sprintf("%s f0 - mean: %f, std %f, min %f, max %f\n%0.1f-%0.1f percentiles: [%f - %f]\n%0.1f-%0.1f percentiles: [%f - %f]\n",NAME,mean(f0),std(f0),min(f0),max(f0),delta1,100-delta1,P1(1),P1(2),delta2,100-delta2,P2(1),P2(2)
));

xlabel ("Hz");
ylabel ("Frames");


line([P1(1);P1(1)],[0; max(NN)],"linestyle","-.", "color", "blue");
line([P1(2);P1(2)],[0; max(NN)],"linestyle","-.", "color", "blue");

line([P2(1);P2(1)],[0; max(NN)],"linestyle","-.", "color", "magenta");
line([P2(2);P2(2)],[0; max(NN)],"linestyle","-.", "color", "magenta");


print("-depsc",epsfilename);

exit;

