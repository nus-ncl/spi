#!/usr/bin/perl

use strict;
use IO::Socket::INET;

shift;

my $subj = shift;
my $addr = shift;

my $sock = new IO::Socket::INET(PeerAddr => 'users.isi.deterlab.net',
    PeerPort => 5555, Proto => 'tcp');

die "Can't open socket: $!\n" unless $sock;

$sock->send("Subject: $subj\n");
$sock->send("To: $addr\n");
while (<>) {
    $sock->send($_);
}
$sock->close();
