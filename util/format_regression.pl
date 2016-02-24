#!/usr/bin/perl

use strict;

use IO::File;
use IO::Dir;
use File::Temp;
use File::Copy;

my $dirname = shift;
my $dir = new IO::Dir($dirname);

while ( my $fn = $dir->read()) {
    next if $fn =~ /^\./;
    my $f = new IO::File("$dirname/$fn");
    my $tf = new File::Temp;
    print $tf "<?xml version='1.0' encoding='utf-8'?>\n";
    print $tf "<exchanges>\n";
    while (<$f>) {
	s/(<\?xml[^\?]*\?>)/<!-- $1 -->/;
	print $tf $_;
    }
    print $tf "</exchanges>\n";
    $f->close();
    $tf->flush();
    move($tf->filename(), "$dirname/$fn");
    $tf->close();
}

=pod

=head1 NAME

B<format_regression.pl> - format Deter API regression test output for XML viewers

=head1 SYNOPSIS

B<perl> B<format_regession.pl> I<regression directory>

=head1 DESCRIPTION

The Deter API regression suite outputs traces of the SOAP exchanges that happen
as the regressions are run.  B<format_regression.pl> removes the embedded xml
declarations and adds a dummy enclosing top level element.  The result can be
viewed directly and clearly a web browser.

B<format_regression.pl> takes one required argument, the directory containing
the regression output.  Each file is assumed to be a file of XML exchanges.
Each file is replaced with one with a single xml declaration and and enclosing
XML top level element (called <exchanges>..</exchanges>).  
Internal xml declarations are
commented out.

=head1 AUTHORS

Ted Faber <faber@isi.edu>

