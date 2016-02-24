#!/usr/bin/perl


use strict;

use IO::Pipe;
use IO::Socket::INET;

my $ss = new IO::Socket::INET(LocalPort => 5555, Listen=>5);
my $s;

while (1) {
    $s = $ss->accept();
    my $pipe = new IO::Pipe();
    my $subj = <$s>;
    chomp $subj;
    die "Bad subject " unless $subj =~ s/^Subject: //;
    my $addr = <$s>;
    chomp $addr;
    die "Bad address " unless $addr =~ s/^To: //;

    $pipe->writer(('mail', '-s', $subj, $addr));

    while (<$s>) {
	print $pipe $_;
    }
    $s->close();
    $pipe->close();
    my $ts = `date`;
    chomp $ts;
    print "$ts mail sent to $addr with subject '$subj'\n";
}
