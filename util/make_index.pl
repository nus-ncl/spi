#!/usr/bin/perl

use strict;

use IO::File;
use IO::Dir;

my $dirname = shift;
my $dir = new IO::Dir($dirname);
my $f = new IO::File(">$dirname/index.html") || die "Can't open index.html\n";

print $f "<html><body><ul>\n";
while ( my $fn = $dir->read()) {
    next if $fn =~ /^\./ || $fn =~ /^index.html$/;
    print $f "<li><a href=\"$fn\">$fn</a></li>\n";
}
print $f "</ul></body></html>\n";
$f->close();

=pod

=head1 NAME

B<make_index.pl> - add a simple index.html to the DeterAPI regression directory

=head1 SYNOPSIS

B<perl> B<make_index.pl> I<regression directory>

=head1 DESCRIPTION

The Deter API regression suite outputs traces of the SOAP exchanges that happen
as the regressions are run.  B<make_index.pl> creates a very simple html file
thcontaining named links to teh various regression files.

B<make_index.pl> takes one required argument, the directory containing
the regression output.

=head1 AUTHORS

Ted Faber <faber@isi.edu>

